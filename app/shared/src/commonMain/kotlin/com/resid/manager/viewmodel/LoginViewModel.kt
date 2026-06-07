package com.resid.manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.dto.LogementDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceSummaryItem
import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserRole
import com.resid.manager.repository.AuthRepository
import com.resid.manager.repository.LogementRepository
import com.resid.manager.repository.ResidenceRepository
import com.resid.manager.usecase.SearchResidencesUseCase
import com.resid.manager.validation.AuthValidator
import com.resid.manager.network.ApiClient
import io.ktor.client.request.*
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isSearching: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val residenceRepository: ResidenceRepository,
    private val logementRepository: LogementRepository,
    private val searchResidencesUseCase: SearchResidencesUseCase,
    private val sessionStorage: SessionStorage? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        // Load session if available on startup
        sessionStorage?.loadSession()?.let { (token, userName) ->
            val nameParts = userName.split(" ")
            val fName = nameParts.getOrNull(0) ?: "Utilisateur"
            val lName = nameParts.getOrNull(1) ?: "ResidManager"
            _uiState.update { 
                it.copy(
                    jwtToken = token,
                    currentScreen = AuthScreen.MAIN, // go directly to main authenticated screen
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

    fun fetchResidences() {
        val token = _uiState.value.jwtToken ?: return
        
        viewModelScope.launch {
            try {
                residenceRepository.fetchResidences(token)
                    .onSuccess { directory ->
                        val ownedContexts = directory.ownedResidences.map {
                            ResidenceContext(
                                residenceId = it.id,
                                residenceName = it.name,
                                residenceAddress = it.address,
                                userRoleInResidence = UserRole.ADMIN, // OWNER/ADMIN
                                totalUnits = it.totalUnits
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
                                totalUnits = it.totalUnits
                            )
                        }
                        
                        val allResidences = ownedContexts + associatedContexts
                        _uiState.update {
                            it.copy(
                                residences = allResidences,
                                selectedResidenceContext = it.selectedResidenceContext ?: allResidences.firstOrNull()
                            )
                        }
                        fetchLogements()
                    }
                    .onFailure { exception ->
                        _uiState.update { it.copy(errorMessage = "Impossible de récupérer vos résidences : ${exception.message}") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun fetchLogements() {
        val token = _uiState.value.jwtToken ?: return
        val residenceId = _uiState.value.selectedResidenceContext?.residenceId ?: return

        viewModelScope.launch {
            try {
                logementRepository.fetchLogements(token, residenceId)
                    .onSuccess { list ->
                        _uiState.update { it.copy(logements = list) }
                    }
                    .onFailure { exception ->
                        _uiState.update { it.copy(errorMessage = "Impossible de récupérer les logements : ${exception.message}") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur réseau lors de la récupération des logements : ${e.message}") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true, errorMessage = null) }

        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(500) // Debounce delay
                val token = _uiState.value.jwtToken ?: return@launch
                
                searchResidencesUseCase(token, query)
                    .onSuccess { results ->
                        _uiState.update { it.copy(searchResults = results, isSearching = false) }
                    }
                    .onFailure { exception ->
                        _uiState.update { it.copy(errorMessage = exception.message, isSearching = false) }
                    }
            } catch (e: Exception) {
                // Ignore cancellation exceptions safely
            }
        }
    }

    fun createLogement(
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double
    ) {
        val token = _uiState.value.jwtToken ?: return
        val residenceId = _uiState.value.selectedResidenceContext?.residenceId ?: return

        if (name.isBlank() || floor.isBlank() || type.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez remplir tous les champs obligatoires.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = com.resid.manager.dto.LogementCreateRequest(
                    name = name,
                    floor = floor,
                    type = type,
                    nominalRent = nominalRent,
                    serviceCharges = serviceCharges,
                    initialElectricityIndex = initialElectricityIndex
                )

                logementRepository.createLogement(token, residenceId, request)
                    .onSuccess {
                        fetchLogements() // Refresh dashboard / logements list
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showCreateLogementDialog = false,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteLogement(logementId: String) {
        val token = _uiState.value.jwtToken ?: return
        val residenceId = _uiState.value.selectedResidenceContext?.residenceId ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                logementRepository.deleteLogement(token, residenceId, logementId)
                    .onSuccess {
                        fetchLogements() // Refresh dashboard / logements list
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
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
        initialElectricityIndex: Double
    ) {
        val token = _uiState.value.jwtToken ?: return
        val residenceId = _uiState.value.selectedResidenceContext?.residenceId ?: return

        if (name.isBlank() || floor.isBlank() || type.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez remplir tous les champs obligatoires.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = com.resid.manager.dto.LogementCreateRequest(
                    name = name,
                    floor = floor,
                    type = type,
                    nominalRent = nominalRent,
                    serviceCharges = serviceCharges,
                    initialElectricityIndex = initialElectricityIndex
                )

                logementRepository.updateLogement(token, residenceId, logementId, request)
                    .onSuccess {
                        fetchLogements() // Refresh dashboard / logements list
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordPlain = password, errorMessage = null) }
    }

    fun onFirstNameChanged(firstName: String) {
        _uiState.update { it.copy(firstName = firstName, errorMessage = null) }
    }

    fun onLastNameChanged(lastName: String) {
        _uiState.update { it.copy(lastName = lastName, errorMessage = null) }
    }

    fun onBirthDateChanged(birthDate: String) {
        _uiState.update { it.copy(birthDate = birthDate, errorMessage = null) }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun navigateToLogin() {
        _uiState.update { it.copy(currentScreen = AuthScreen.LOGIN, errorMessage = null) }
    }

    fun navigateToRegister() {
        _uiState.update { it.copy(currentScreen = AuthScreen.REGISTER, errorMessage = null) }
    }

    fun navigateToAppScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentAppScreen = screen, errorMessage = null) }
    }

    fun selectResidence(residence: ResidenceContext) {
        _uiState.update { it.copy(selectedResidenceContext = residence, logements = emptyList()) }
        fetchLogements()
    }

    fun setShowCreateResidenceDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateResidenceDialog = show, errorMessage = null) }
    }

    fun setShowJoinResidenceDialog(show: Boolean) {
        _uiState.update { 
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
        _uiState.update { it.copy(showCreateLogementDialog = show, errorMessage = null) }
    }

    fun createResidence(name: String, address: String, kWhPrice: Double) {
        val token = _uiState.value.jwtToken ?: return
        
        if (name.isBlank() || address.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez remplir tous les champs obligatoires.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceRepository.createResidence(token, com.resid.manager.dto.ResidenceCreateRequest(name, address, "XOF", kWhPrice))
                    .onSuccess { summary ->
                        fetchResidences()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showCreateResidenceDialog = false,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun joinResidence(residenceId: String) {
        val token = _uiState.value.jwtToken ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/join") {
                    header(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
                }

                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showJoinResidenceDialog = false,
                            errorMessage = null
                        )
                    }
                    fetchResidences() // Reload directory list
                } else {
                    val errorBody = response.body<com.resid.manager.dto.ErrorResponse>()
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorBody.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erreur réseau : ${e.message}") }
            }
        }
    }

    fun updateResidence(residenceId: String, name: String, address: String, kWhPrice: Double) {
        val token = _uiState.value.jwtToken ?: return

        if (name.isBlank() || address.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez remplir tous les champs obligatoires.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceRepository.updateResidence(token, residenceId, com.resid.manager.dto.ResidenceCreateRequest(name, address, "XOF", kWhPrice))
                    .onSuccess {
                        fetchResidences()
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteResidence(residenceId: String) {
        val token = _uiState.value.jwtToken ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                residenceRepository.deleteResidence(token, residenceId)
                    .onSuccess {
                        _uiState.update { state ->
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value
        val email = currentState.email
        val password = currentState.passwordPlain

        val validation = AuthValidator.validateLogin(email, password)
        if (validation.isFailure) {
            _uiState.update { it.copy(errorMessage = validation.exceptionOrNull()?.message) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                authRepository.login(email, password)
                    .onSuccess { authResponse ->
                        val nameParts = authResponse.user.name.split(" ")
                        val fName = nameParts.getOrNull(0) ?: "Utilisateur"
                        val lName = nameParts.getOrNull(1) ?: ""
                        _uiState.update {
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun register() {
        val currentState = _uiState.value
        val email = currentState.email
        val password = currentState.passwordPlain
        val firstName = currentState.firstName
        val lastName = currentState.lastName
        val birthDate = currentState.birthDate.ifBlank { null }
        val phone = currentState.phone.ifBlank { null }

        val validation = AuthValidator.validateRegister(firstName, lastName, email, password)
        if (validation.isFailure) {
            _uiState.update { it.copy(errorMessage = validation.exceptionOrNull()?.message) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                authRepository.register(firstName, lastName, birthDate, phone, email, password)
                    .onSuccess { authResponse ->
                        _uiState.update {
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur réseau : ${e.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        _uiState.update {
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
                currentAppScreen = AppScreen.DASHBOARD
            )
        }
        sessionStorage?.clearSession()
    }
}
