package com.resid.manager.viewmodel

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.*
import com.resid.manager.repository.*
import com.resid.manager.usecase.SearchResidencesUseCase
import com.resid.manager.validation.AuthValidator
import com.resid.manager.network.ApiClient
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.contentType
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreen {
    LOGIN,
    REGISTER,
    MAIN
}

enum class AppScreen(val title: String) {
    DASHBOARD("Tableau de bord"),
    RESIDENCES("Propriétés & Résidences"),
    LOGEMENTS("Unités / Logements"),
    BAUX("Contrats de bail"),
    MEMBERS("Membres & Habilitations"),
    ELECTRICITY("Facturation Électricité"),
    TICKETS("Tickets d'Intervention"),
    FINANCES("Cashflow & Finances"),
    PROFILE("Mon Compte")
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
    
    // Logements/Units state
    val logements: List<LogementDto> = emptyList(),
    val showCreateLogementDialog: Boolean = false,

    // Real-time debounced search states for JoinResidence
    val searchQuery: String = "",
    val searchResults: List<ResidenceSummaryItem> = emptyList(),
    val isSearching: Boolean = false,

    // Leases state
    val leases: List<LeaseDto> = emptyList(),

    // Members list state
    val members: List<ResidenceMemberSummary> = emptyList(),

    // Dark/Light theme state
    val darkMode: Boolean = false,

    // Language state (e.g. "fr", "en")
    val language: String = "fr",

    // Predefined equipments list
    val availableEquipements: List<EquipementDto> = emptyList()
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
    
    data class CreateLogement(val name: String, val floor: String, val type: String, val nominalRent: Double, val serviceCharges: Double, val initialIndex: Double, val equipementIds: List<String> = emptyList()) : LoginIntent
    data class UpdateLogement(val id: String, val name: String, val floor: String, val type: String, val rent: Double, val charges: Double, val initialIndex: Double, val equipementIds: List<String> = emptyList()) : LoginIntent
    data class DeleteLogement(val id: String) : LoginIntent
    
    data class CreateLease(val logementId: String, val request: LeaseCreateRequest, val onSuccess: () -> Unit) : LoginIntent
    data class RecordLeasePayment(val leaseId: String, val amount: Double, val category: String, val onResult: (Result<LeaseDto>) -> Unit) : LoginIntent
    data class UpdateLeaseStatus(val leaseId: String, val status: LeaseStatus, val onResult: (Result<LeaseDto>) -> Unit) : LoginIntent
    
    data class SearchResidences(val query: String) : LoginIntent
    data class NavigateToAppScreen(val screen: AppScreen) : LoginIntent
    data class SetShowCreateResidenceDialog(val show: Boolean) : LoginIntent
    data class SetShowJoinResidenceDialog(val show: Boolean) : LoginIntent
    data class SetShowCreateLogementDialog(val show: Boolean) : LoginIntent
    data object ToggleTheme : LoginIntent
}

sealed interface LoginEffect {
    data class ShowToast(val message: String) : LoginEffect
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val residenceRepository: ResidenceRepository,
    private val logementRepository: LogementRepository,
    private val leaseRepository: LeaseRepository,
    private val memberRepository: MemberRepository,
    private val searchResidencesUseCase: SearchResidencesUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<LoginUiState, LoginIntent, LoginEffect>(LoginUiState()) {

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        fetchEquipements()
        // Load session if available on startup
        sessionStorage?.loadSession()?.let { (token, userName) ->
            val nameParts = userName.split(" ")
            val fName = nameParts.getOrNull(0) ?: "Utilisateur"
            val lName = nameParts.getOrNull(1) ?: "ResidManager"
            updateState { 
                it.copy(
                    jwtToken = token,
                    currentScreen = AuthScreen.MAIN,
                    currentAppScreen = AppScreen.DASHBOARD,
                    firstName = fName,
                    lastName = lName,
                    loggedInUser = UserDto(
                        id = "",
                        email = "",
                        name = userName,
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
            is LoginIntent.CreateLogement -> createLogement(intent.name, intent.floor, intent.type, intent.nominalRent, intent.serviceCharges, intent.initialIndex, intent.equipementIds)
            is LoginIntent.UpdateLogement -> updateLogement(intent.id, intent.name, intent.floor, intent.type, intent.rent, intent.charges, intent.initialIndex, intent.equipementIds)
            is LoginIntent.DeleteLogement -> deleteLogement(intent.id)
            is LoginIntent.CreateLease -> createLease(intent.logementId, intent.request, intent.onSuccess)
            is LoginIntent.RecordLeasePayment -> recordLeasePayment(intent.leaseId, intent.amount, intent.category, intent.onResult)
            is LoginIntent.UpdateLeaseStatus -> updateLeaseStatus(intent.leaseId, intent.status, intent.onResult)
            is LoginIntent.SearchResidences -> onSearchQueryChanged(intent.query)
            is LoginIntent.NavigateToAppScreen -> navigateToAppScreen(intent.screen)
            is LoginIntent.SetShowCreateResidenceDialog -> setShowCreateResidenceDialog(intent.show)
            is LoginIntent.SetShowJoinResidenceDialog -> setShowJoinResidenceDialog(intent.show)
            is LoginIntent.SetShowCreateLogementDialog -> setShowCreateLogementDialog(intent.show)
            is LoginIntent.ToggleTheme -> toggleTheme()
        }
    }

    fun toggleTheme() {
        updateState { it.copy(darkMode = !it.darkMode) }
    }

    fun setLanguage(lang: String) {
        updateState { it.copy(language = lang) }
    }

    fun fetchEquipements() {
        viewModelScope.launch {
            try {
                val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/equipements")
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    val list = response.body<List<EquipementDto>>()
                    updateState { it.copy(availableEquipements = list) }
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
    fun setShowCreateLogementDialog(show: Boolean) {
        updateState { it.copy(showCreateLogementDialog = show, errorMessage = null) }
    }

    // Public actions to preserve classic direct UI invocations and support stateless screens

    fun fetchResidences() {
        fetchEquipements()
        val token = uiState.value.jwtToken ?: return
        
        viewModelScope.launch {
            try {
                residenceRepository.fetchResidences(token)
                    .onSuccess { directory ->
                        val ownedContexts = directory.ownedResidences.map {
                            ResidenceContext(
                                residenceId = it.id,
                                residenceName = it.name,
                                residenceAddress = it.address,
                                userRoleInResidence = UserRole.ADMIN,
                                totalUnits = it.totalUnits,
                                currencySymbol = it.currencySymbol,
                                currencyCode = it.currencyCode
                            )
                        }
                        val associatedContexts = directory.associatedResidences.map {
                            val roleEnum = try {
                                UserRole.valueOf(it.role)
                            } catch (e: Exception) {
                                UserRole.TENANT
                            }
                            ResidenceContext(
                                residenceId = it.id,
                                residenceName = it.name,
                                residenceAddress = it.address,
                                userRoleInResidence = roleEnum,
                                totalUnits = it.totalUnits,
                                currencySymbol = it.currencySymbol,
                                currencyCode = it.currencyCode
                            )
                        }
                        
                        val allResidences = ownedContexts + associatedContexts
                        updateState {
                            it.copy(
                                residences = allResidences,
                                selectedResidenceContext = it.selectedResidenceContext ?: allResidences.firstOrNull()
                            )
                        }
                        fetchLogements()
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

    fun fetchLogements() {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return

        viewModelScope.launch {
            try {
                logementRepository.fetchLogements(token, residenceId)
                    .onSuccess { list ->
                        updateState { it.copy(logements = list) }
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
        updateState { it.copy(selectedResidenceContext = residence, logements = emptyList(), leases = emptyList(), members = emptyList()) }
        fetchLogements()
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
                residenceRepository.createResidence(token, ResidenceCreateRequest(name, address, defaultCurrency, kWhPrice))
                    .onSuccess {
                        fetchResidences()
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
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/join") {
                    header(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
                }
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
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
                residenceRepository.updateResidence(token, residenceId, ResidenceCreateRequest(name, address, "XOF", kWhPrice))
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

    fun createLogement(
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double,
        equipementIds: List<String> = emptyList()
    ) {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = LogementCreateRequest(name, floor, type, nominalRent, serviceCharges, initialElectricityIndex, equipementIds)
                logementRepository.createLogement(token, residenceId, request)
                    .onSuccess {
                        fetchLogements()
                        updateState { it.copy(isLoading = false, showCreateLogementDialog = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        updateState { it.copy(isLoading = false, errorMessage = exception.message) }
                    }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun deleteLogement(logementId: String) {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                logementRepository.deleteLogement(token, residenceId, logementId)
                    .onSuccess {
                        fetchLogements()
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

    fun updateLogement(
        logementId: String,
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double,
        equipementIds: List<String> = emptyList()
    ) {
        val token = uiState.value.jwtToken ?: return
        val residenceId = uiState.value.selectedResidenceContext?.residenceId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = LogementCreateRequest(name, floor, type, nominalRent, serviceCharges, initialElectricityIndex, equipementIds)
                logementRepository.updateLogement(token, residenceId, logementId, request)
                    .onSuccess {
                        fetchLogements()
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

    fun createLease(logementId: String, request: LeaseCreateRequest, onSuccess: () -> Unit) {
        val token = uiState.value.jwtToken ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                leaseRepository.createLease(token, logementId, request)
                    .onSuccess {
                        fetchLeases()
                        fetchLogements()
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

    fun updateLeaseStatus(leaseId: String, status: LeaseStatus, onResult: (Result<LeaseDto>) -> Unit) {
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
                        val nameParts = authResponse.user.name.split(" ")
                        val fName = nameParts.getOrNull(0) ?: "Utilisateur"
                        val lName = nameParts.getOrNull(1) ?: ""
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
                        sessionStorage?.saveSession(authResponse.token, authResponse.user.name)
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
                        sessionStorage?.saveSession(authResponse.token, authResponse.user.name)
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
                logements = emptyList(),
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
        val nameParts = user.name.split(" ")
        val fName = nameParts.firstOrNull() ?: ""
        val lName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""
        updateState { 
            it.copy(
                loggedInUser = user, 
                firstName = fName, 
                lastName = lName, 
                phone = user.phone ?: ""
            ) 
        }
    }
}
