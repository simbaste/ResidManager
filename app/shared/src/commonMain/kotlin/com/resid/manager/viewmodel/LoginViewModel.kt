package com.resid.manager.viewmodel

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ApplicationRequest
import com.resid.manager.dto.CurrencyCodeDto
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceSummaryItemDto
import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.RoleDto
import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserRole
import com.resid.manager.dto.toUserRole
import com.resid.manager.network.ApiClient
import com.resid.manager.repository.AuthRepository
import com.resid.manager.repository.LeaseRepository
import com.resid.manager.repository.MemberRepository
import com.resid.manager.repository.ResidenceRepository
import com.resid.manager.repository.ResidenceUnitRepository
import com.resid.manager.usecase.SearchResidencesUseCase
import com.resid.manager.validation.AuthValidator
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.launch

enum class AuthScreen {
    LOGIN,
    REGISTER,
    MAIN
}

enum class AppScreen(val title: String, val navPath: String) {
    DASHBOARD("Tableau de bord", "dashboard"),
    RESIDENCES("Propriétés & Résidences", "residences"),
    UNITS("Unités / Logements", "units"),
    LEASES("Contrats de bail", "leases"),
    MEMBERS("Membres & Habilitations", "members"),
    ELECTRICITY("Facturation Électricité", "electricity"),
    TICKETS("Tickets d'Intervention", "tickets"),
    FINANCES("Cashflow & Finances", "finances"),
    PROFILE("Mon Compte", "profil")
}

data class LoginUiState(
    val currentScreen: AuthScreen = AuthScreen.LOGIN,
    val currentAppScreen: AppScreen = AppScreen.DASHBOARD,
    val email: String = "",
    val passwordPlain: String = "",
    val passwordVisible: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: UserDto? = null,
    val jwtToken: String? = null,
    
    // Onboarding and Residence selector state
    val residences: List<ResidenceContext> = emptyList(),
    val selectedResidenceContext: ResidenceContext? = null,
    val showCreateResidenceDialog: Boolean = false,
    val showJoinResidenceDialog: Boolean = false,
    
    // ResidenceUnits state
    val residenceUnits: List<ResidenceUnitDto> = emptyList(),
    val showCreateUnitDialog: Boolean = false,

    // Real-time debounced search states for JoinResidence
    val searchQuery: String = "",
    val searchResults: List<ResidenceSummaryItemDto> = emptyList(),
    val isSearching: Boolean = false,

    // Leases state
    val leases: List<LeaseDto> = emptyList(),

    // Members list state
    val members: List<ResidenceMemberSummaryDto> = emptyList(),

    // Dark/Light theme state
    val darkMode: Boolean = false,

    // Language state (e.g. "fr", "en")
    val language: String = "fr",

    // Predefined equipments list
    val availableEquipments: List<EquipmentDto> = emptyList()
)

sealed interface LoginIntent {
    data class LoginAction(val email: String, val passwordPlain: String) : LoginIntent
    data class RegisterAction(val firstName: String, val lastName: String, val birthDate: String?, val phone: String?, val email: String, val passwordPlain: String) : LoginIntent
    data object LogoutAction : LoginIntent
    
    data class CreateResidence(val name: String, val address: String, val defaultCurrency: String, val kWhPrice: Double) : LoginIntent
    data class JoinResidence(val residenceId: String) : LoginIntent
    data class UpdateResidence(val residenceId: String, val name: String, val address: String, val kWhPrice: Double) : LoginIntent
    data class DeleteResidence(val residenceId: String) : LoginIntent
    data class SelectResidence(val residence: ResidenceContext) : LoginIntent
    
    data class CreateResidenceUnit(val name: String, val floor: String, val type: String, val nominalRent: Double, val serviceCharges: Double, val initialIndex: Double, val equipmentIds: List<String> = emptyList()) : LoginIntent
    data class UpdateResidenceUnit(val id: String, val name: String, val floor: String, val type: String, val rent: Double, val charges: Double, val initialIndex: Double, val equipmentIds: List<String> = emptyList()) : LoginIntent
    data class DeleteResidenceUnit(val id: String) : LoginIntent
    
    data class CreateLease(val residenceUnitId: String, val request: LeaseCreateRequest, val onSuccess: () -> Unit) : LoginIntent
    data class RecordLeasePayment(val leaseId: String, val amount: Double, val category: String, val onResult: (Result<LeaseDto>) -> Unit) : LoginIntent
    data class UpdateLeaseStatus(val leaseId: String, val status: LeaseStatusDto, val onResult: (Result<LeaseDto>) -> Unit) : LoginIntent
    
    data class SearchResidences(val query: String) : LoginIntent
    data class NavigateToAppScreen(val screen: AppScreen) : LoginIntent
    data class SetShowCreateResidenceDialog(val show: Boolean) : LoginIntent
    data class SetShowJoinResidenceDialog(val show: Boolean) : LoginIntent
    data class SetShowCreateUnitDialog(val show: Boolean) : LoginIntent
    data object ToggleTheme : LoginIntent
}

sealed interface LoginEffect {
    data class ShowToast(val message: String) : LoginEffect
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val residenceRepository: ResidenceRepository,
    private val residenceUnitRepository: ResidenceUnitRepository,
    private val leaseRepository: LeaseRepository,
    private val memberRepository: MemberRepository,
    private val searchResidencesUseCase: SearchResidencesUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<LoginUiState, LoginIntent, LoginEffect>(LoginUiState()) {

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        fetchEquipments()
        // Load session if available on startup
        sessionStorage?.loadSession()?.let { session ->
            updateState {
                it.copy(
                    jwtToken = session.token,
                    currentScreen = AuthScreen.MAIN,
                    currentAppScreen = AppScreen.DASHBOARD,
                    firstName = session.firstName,
                    lastName = session.lastName,
                    loggedInUser = UserDto(
                        id = "",
                        email = "",
                        firstName = session.firstName,
                        lastName = session.lastName,
                        phone = null,
                        birthDate = null,
                        createdAt = "",
                        updatedAt = ""
                    )
                ) 
            }
            fetchResidences()
        }
    }

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.LoginAction -> login(intent.email, intent.passwordPlain)
            is LoginIntent.RegisterAction -> register(intent.firstName, intent.lastName, intent.birthDate, intent.phone, intent.email, intent.passwordPlain)
            is LoginIntent.LogoutAction -> logout()
            is LoginIntent.CreateResidence -> createResidence(intent.name, intent.address, intent.defaultCurrency, intent.kWhPrice)
            is LoginIntent.JoinResidence -> joinResidence(intent.residenceId)
            is LoginIntent.UpdateResidence -> updateResidence(intent.residenceId, intent.name, intent.address, intent.kWhPrice)
            is LoginIntent.DeleteResidence -> deleteResidence(intent.residenceId)
            is LoginIntent.SelectResidence -> selectResidence(intent.residence)
            is LoginIntent.CreateResidenceUnit -> createResidenceUnit(intent.name, intent.floor, intent.type, intent.nominalRent, intent.serviceCharges, intent.initialIndex, intent.equipmentIds)
            is LoginIntent.UpdateResidenceUnit -> updateResidenceUnit(intent.id, intent.name, intent.floor, intent.type, intent.rent, intent.charges, intent.initialIndex, intent.equipmentIds)
            is LoginIntent.DeleteResidenceUnit -> deleteResidenceUnit(intent.id)
            is LoginIntent.CreateLease -> createLease(intent.residenceUnitId, intent.request, intent.onSuccess)
            is LoginIntent.RecordLeasePayment -> recordLeasePayment(intent.leaseId, intent.amount, intent.category, intent.onResult)
            is LoginIntent.UpdateLeaseStatus -> updateLeaseStatus(intent.leaseId, intent.status, intent.onResult)
            is LoginIntent.SearchResidences -> onSearchQueryChanged(intent.query)
            is LoginIntent.NavigateToAppScreen -> navigateToAppScreen(intent.screen)
            is LoginIntent.SetShowCreateResidenceDialog -> setShowCreateResidenceDialog(intent.show)
            is LoginIntent.SetShowJoinResidenceDialog -> setShowJoinResidenceDialog(intent.show)
            is LoginIntent.SetShowCreateUnitDialog -> setShowCreateUnitDialog(intent.show)
            is LoginIntent.ToggleTheme -> toggleTheme()
        }
    }

    fun toggleTheme() {
        updateState { it.copy(darkMode = !it.darkMode) }
    }

    fun setLanguage(lang: String) {
        updateState { it.copy(language = lang) }
    }

    fun fetchEquipments() {
        viewModelScope.launch {
            try {
                val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/equipements")
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    val list = response.body<List<EquipmentDto>>()
                    updateState { it.copy(availableEquipments = list) }
                }
            } catch (e: Exception) {}
        }
    }

    // Public setters for text fields to fully preserve standard login/register ui bindings
    fun onEmailChanged(email: String) {
        updateState { it.copy(email = email, errorMessage = null) }
    }
    fun onPasswordChanged(password: String) {
        updateState { it.copy(passwordPlain = password, errorMessage = null) }
    }
    fun onFirstNameChanged(firstName: String) {
        updateState { it.copy(firstName = firstName, errorMessage = null) }
    }
    fun onLastNameChanged(lastName: String) {
        updateState { it.copy(lastName = lastName, errorMessage = null) }
    }
    fun onBirthDateChanged(birthDate: String) {
        updateState { it.copy(birthDate = birthDate, errorMessage = null) }
    }
    fun onPhoneChanged(phone: String) {
        updateState { it.copy(phone = phone, errorMessage = null) }
    }
    fun togglePasswordVisibility() {
        updateState { it.copy(passwordVisible = !it.passwordVisible) }
    }
    fun navigateToRegister() {
        updateState { it.copy(currentScreen = AuthScreen.REGISTER, errorMessage = null) }
    }
    fun navigateToLogin() {
        updateState { it.copy(currentScreen = AuthScreen.LOGIN, errorMessage = null) }
    }
    fun navigateToMain(token: String? = null, user: UserDto? = null) {
        val effectiveToken = token ?: uiState.value.jwtToken
        val effectiveUser = user ?: uiState.value.loggedInUser
        updateState { 
            it.copy(
                currentScreen = AuthScreen.MAIN, 
                errorMessage = null,
                jwtToken = effectiveToken ?: it.jwtToken,
                loggedInUser = effectiveUser ?: it.loggedInUser,
                firstName = effectiveUser?.firstName ?: it.firstName,
                lastName = effectiveUser?.lastName ?: it.lastName
            ) 
        }
        fetchResidences(tokenOverride = effectiveToken)
    }
    fun setShowCreateResidenceDialog(show: Boolean) {
        updateState { it.copy(showCreateResidenceDialog = show, errorMessage = null) }
    }
    fun setShowJoinResidenceDialog(show: Boolean) {
        updateState { 
            it.copy(
                showJoinResidenceDialog = show, 
                searchQuery = "", 
                searchResults = emptyList(), 
                isSearching = false, 
                errorMessage = null
            ) 
        }
    }
    fun setShowCreateUnitDialog(show: Boolean) {
        updateState { it.copy(showCreateUnitDialog = show, errorMessage = null) }
    }

    // Public actions to preserve classic direct UI invocations and support stateless screens

    fun fetchResidences(tokenOverride: String? = null, autoSelectResidenceId: String? = null) {
        fetchEquipments()
        val token = tokenOverride ?: uiState.value.jwtToken ?: return
        
        viewModelScope.launch {
            try {
                residenceRepository.fetchResidences(token)
                    .onSuccess { directory ->
                        val ownedContexts = directory.ownedResidences.map {
                            ResidenceContext(
                                residenceId = it.id,
                                residenceName = it.name,
                                residenceAddress = it.address,
                                userRoleInResidence = UserRole.OWNER,
                                totalUnits = it.totalUnits,
                                currencySymbol = it.currencySymbol.label,
                                currencyCode = it.currencyCode.name,
                                kWhPrice = it.kWhPrice
                            )
                        }
                        val associatedContexts = directory.associatedResidences.map {
                            ResidenceContext(
                                residenceId = it.id,
                                residenceName = it.name,
                                residenceAddress = it.address,
                                userRoleInResidence = it.roleDto.toUserRole(),
                                totalUnits = it.totalUnits,
                                currencySymbol = it.currencySymbol.label,
                                currencyCode = it.currencyCode.name,
                                kWhPrice = it.kWhPrice
                            )
                        }
                        
                        val allResidences = ownedContexts + associatedContexts
                        val savedResidenceId = autoSelectResidenceId ?: sessionStorage?.loadLastSelectedResidenceId()

                        updateState { state ->
                            val targetSelection = if (savedResidenceId != null) {
                                allResidences.find { res -> res.residenceId == savedResidenceId } ?: state.selectedResidenceContext ?: allResidences.firstOrNull()
                            } else {
                                state.selectedResidenceContext ?: allResidences.firstOrNull()
                            }
                            state.copy(
                                residences = allResidences,
                                selectedResidenceContext = targetSelection
                            )
                        }
                        fetchResidenceUnits()
                        fetchLeases()
                        fetchMembers()
                    }
                    .onFailure { exception ->
                        updateState { it.copy(errorMessage = "Impossible de récupérer vos résidences : ${exception.message}") }
                    }
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun fetchResidenceUnits() {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return

        viewModelScope.launch {
            try {
                residenceUnitRepository.fetchResidenceUnits(token, residenceId)
                    .onSuccess { list ->
                        updateState { it.copy(residenceUnits = list) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(errorMessage = "Impossible de récupérer les logements : ${exception.message}") }
                    }
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "Erreur réseau lors de la récupération des logements : ${e.message}") }
            }
        }
    }

    fun fetchLeases() {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return

        viewModelScope.launch {
            try {
                leaseRepository.fetchLeases(token, residenceId)
                    .onSuccess { list ->
                        updateState { it.copy(leases = list) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "Erreur réseau lors du chargement des baux : ${e.message}") }
            }
        }
    }

    fun fetchMembers() {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return

        viewModelScope.launch {
            try {
                memberRepository.fetchMembers(token, residenceId)
                    .onSuccess { list ->
                        updateState { it.copy(members = list) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "Erreur réseau lors du chargement des membres : ${e.message}") }
            }
        }
    }

    fun selectResidence(residence: ResidenceContext) {
        sessionStorage?.saveLastSelectedResidenceId(residence.residenceId)
        updateState { it.copy(selectedResidenceContext = residence, residenceUnits = emptyList(), leases = emptyList(), members = emptyList()) }
        fetchResidenceUnits()
        fetchLeases()
        fetchMembers()
    }

    fun navigateToAppScreen(screen: AppScreen) {
        updateState { it.copy(currentAppScreen = screen, errorMessage = null) }
    }

    fun createResidence(name: String, address: String, defaultCurrency: String, kWhPrice: Double) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceRepository.createResidence(token, ResidenceCreateRequest(name, address,
                    CurrencyCodeDto.valueOf(defaultCurrency), kWhPrice))
                    .onSuccess { created ->
                        fetchResidences(autoSelectResidenceId = created.id)
                        updateState { it.copy(isLoading = false, showCreateResidenceDialog = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun joinResidence(residenceId: String) {
        val token = uiState.value.jwtToken ?: return
        val currentUserId = uiState.value.loggedInUser?.id ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val req = ApplicationRequest(role = RoleDto.TENANT)
                val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/applications?residenceId=$residenceId&userId=$currentUserId") {
                    header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
                    header(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
                    setBody(req)
                }
                if (response.status == io.ktor.http.HttpStatusCode.Created || response.status == io.ktor.http.HttpStatusCode.OK) {
                    updateState { it.copy(isLoading = false, showJoinResidenceDialog = false, errorMessage = null) }
                    fetchResidences()
                } else {
                    val errorBody = response.body<ErrorResponse>()
                    updateState { it.copy(isLoading = false, errorMessage = errorBody.message) }
                }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun updateResidence(residenceId: String, name: String, address: String, kWhPrice: Double) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceRepository.updateResidence(token, residenceId, ResidenceCreateRequest(name, address,
                    CurrencyCodeDto.XOF, kWhPrice))
                    .onSuccess {
                        fetchResidences()
                        updateState { it.copy(isLoading = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun deleteResidence(residenceId: String) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceRepository.deleteResidence(token, residenceId)
                    .onSuccess {
                        updateState { state ->
                            val updatedResidences = state.residences.filter { it.residenceId != residenceId }
                            state.copy(
                                isLoading = false,
                                residences = updatedResidences,
                                selectedResidenceContext = updatedResidences.firstOrNull(),
                                errorMessage = null
                            )
                        }
                        fetchResidences()
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun createResidenceUnit(
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double,
        equipmentIds: List<String> = emptyList()
    ) {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = ResidenceUnitCreateRequest(name, floor, type, nominalRent, serviceCharges, initialElectricityIndex, equipmentIds)
                residenceUnitRepository.createResidenceUnit(token, residenceId, request)
                    .onSuccess {
                        fetchResidenceUnits()
                        updateState { it.copy(isLoading = false, showCreateUnitDialog = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun deleteResidenceUnit(residenceUnitId: String) {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceUnitRepository.deleteResidenceUnit(token, residenceId, residenceUnitId)
                    .onSuccess {
                        fetchResidenceUnits()
                        updateState { it.copy(isLoading = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun updateResidenceUnit(
        residenceUnitId: String,
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double,
        equipmentIds: List<String> = emptyList()
    ) {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = ResidenceUnitCreateRequest(name, floor, type, nominalRent, serviceCharges, initialElectricityIndex, equipmentIds)
                residenceUnitRepository.updateResidenceUnit(token, residenceId, residenceUnitId, request)
                    .onSuccess {
                        fetchResidenceUnits()
                        updateState { it.copy(isLoading = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun createLease(residenceUnitId: String, request: LeaseCreateRequest, onSuccess: () -> Unit) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                leaseRepository.createLease(token, residenceUnitId, request)
                    .onSuccess {
                        fetchLeases()
                        fetchResidenceUnits()
                        fetchMembers()
                        updateState { it.copy(isLoading = false, errorMessage = null) }
                        onSuccess()
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun recordLeasePayment(leaseId: String, amount: Double, category: String, onResult: (Result<LeaseDto>) -> Unit) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                leaseRepository.recordLeasePayment(token, leaseId, amount, category)
                    .onSuccess { updated ->
                        fetchLeases()
                        updateState { it.copy(isLoading = false, errorMessage = null) }
                        onResult(Result.success(updated))
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                        onResult(Result.failure(exception))
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
                onResult(Result.failure(e))
            }
        }
    }

    fun updateLeaseStatus(leaseId: String, status: LeaseStatusDto, onResult: (Result<LeaseDto>) -> Unit) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                leaseRepository.updateLeaseStatus(token, leaseId, status)
                    .onSuccess { updated ->
                        fetchLeases()
                        updateState { it.copy(isLoading = false, errorMessage = null) }
                        onResult(Result.success(updated))
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                        onResult(Result.failure(exception))
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
                onResult(Result.failure(e))
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query, isSearching = true, errorMessage = null) }

        searchJob?.cancel()
        if (query.length < 2) {
            updateState { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(500)
                val token = uiState.value.jwtToken ?: return@launch
                
                searchResidencesUseCase(token, query)
                    .onSuccess { results ->
                        updateState { it.copy(searchResults = results, isSearching = false) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(errorMessage = exception.message, isSearching = false) }
                    }
            } catch (e: Exception) {
                // Safely handle cancellation
            }
        }
    }

    fun login() {
        val currentState = uiState.value
        login(currentState.email, currentState.passwordPlain)
    }

    private fun login(emailInput: String, passwordInput: String) {
        val validation = AuthValidator.validateLogin(emailInput, passwordInput)
        if (validation.isFailure) {
            updateState { it.copy(errorMessage = validation.exceptionOrNull()?.message) }
            return
        }

        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                authRepository.login(emailInput, passwordInput)
                    .onSuccess { authResponse ->
                        val fName = authResponse.user.firstName ?: "Utilisateur"
                        val lName = authResponse.user.lastName ?: ""
                        updateState {
                            it.copy(
                                isLoading = false,
                                jwtToken = authResponse.token,
                                loggedInUser = authResponse.user,
                                firstName = fName,
                                lastName = lName,
                                currentScreen = AuthScreen.MAIN,
                                currentAppScreen = AppScreen.DASHBOARD
                            )
                        }
                        sessionStorage?.saveSession(
                            authResponse.token,
                            authResponse.refreshToken,
                            fName,
                            lName
                        )
                        fetchResidences()
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun register() {
        val currentState = uiState.value
        register(
            currentState.firstName, 
            currentState.lastName, 
            currentState.birthDate.ifBlank { null }, 
            currentState.phone.ifBlank { null }, 
            currentState.email, 
            currentState.passwordPlain
        )
    }

    private fun register(fName: String, lName: String, bDate: String?, phoneNum: String?, emailInput: String, passwordInput: String) {
        val validation = AuthValidator.validateRegister(fName, lName, emailInput, passwordInput)
        if (validation.isFailure) {
            updateState { it.copy(errorMessage = validation.exceptionOrNull()?.message) }
            return
        }

        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                authRepository.register(fName, lName, bDate, phoneNum, emailInput, passwordInput)
                    .onSuccess { authResponse ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                jwtToken = authResponse.token,
                                loggedInUser = authResponse.user,
                                currentScreen = AuthScreen.MAIN,
                                currentAppScreen = AppScreen.DASHBOARD
                            )
                        }
                        val fName = authResponse.user.firstName ?: "Utilisateur"
                        val lName = authResponse.user.lastName ?: ""
                        sessionStorage?.saveSession(
                            authResponse.token,
                            authResponse.refreshToken,
                            fName,
                            lName
                        )
                        fetchResidences()
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun logout() {
        updateState {
            it.copy(
                email = "",
                passwordPlain = "",
                firstName = "",
                lastName = "",
                birthDate = "",
                phone = "",
                jwtToken = null,
                loggedInUser = null,
                residences = emptyList(),
                selectedResidenceContext = null,
                residenceUnits = emptyList(),
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                currentScreen = AuthScreen.LOGIN,
                currentAppScreen = AppScreen.DASHBOARD,
                leases = emptyList(),
                members = emptyList()
            )
        }
        sessionStorage?.clearSession()
    }

    fun updateUserProfile(user: UserDto) {
        updateState { 
            it.copy(
                loggedInUser = user, 
                firstName = user.firstName.orEmpty(),
                lastName = user.lastName.orEmpty(),
                phone = user.phone ?: ""
            ) 
        }
    }
}
