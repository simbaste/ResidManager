package com.resid.manager.features.dashboard

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.features.dashboard.mvi.DashboardEffect
import com.resid.manager.features.dashboard.mvi.DashboardIntent
import com.resid.manager.features.dashboard.mvi.DashboardUiState
import com.resid.manager.features.dashboard.usecase.GetDashboardDataUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class DashboardViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<DashboardUiState, DashboardIntent, DashboardEffect>(DashboardUiState()) {

    private var loadJob: Job? = null

    init {
        fetchDefaultPeriod()
        sessionStorage?.loadSession()?.let { session ->
            updateState { it.copy(firstName = session.firstName) }
            val lastResId = sessionStorage.loadLastSelectedResidenceId()
            if (!lastResId.isNullOrBlank()) {
                loadResidenceAndData(session.token, lastResId)
            }
        }
    }

    override fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.Refresh -> {
                val session = sessionStorage?.loadSession()
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (session != null && !residenceId.isNullOrBlank()) {
                    fetchData(session.token, residenceId, uiState.value.periodFilter, uiState.value.customStartText, uiState.value.customEndText)
                }
            }
            is DashboardIntent.LoadData -> {
                val session = sessionStorage?.loadSession()
                if (session != null) {
                    loadResidenceAndData(session.token, intent.residenceId)
                }
            }
            is DashboardIntent.ChangePeriodFilter -> {
                updateState { it.copy(periodFilter = intent.filter) }
                val session = sessionStorage?.loadSession()
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (session != null && !residenceId.isNullOrBlank()) {
                    fetchData(session.token, residenceId, intent.filter, uiState.value.customStartText, uiState.value.customEndText)
                }
            }
            is DashboardIntent.UpdateCustomStart -> {
                updateState { it.copy(customStartText = intent.startDate) }
                val session = sessionStorage?.loadSession()
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (session != null && !residenceId.isNullOrBlank()) {
                    fetchData(session.token, residenceId, uiState.value.periodFilter, intent.startDate, uiState.value.customEndText)
                }
            }
            is DashboardIntent.UpdateCustomEnd -> {
                updateState { it.copy(customEndText = intent.endDate) }
                val session = sessionStorage?.loadSession()
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (session != null && !residenceId.isNullOrBlank()) {
                    fetchData(session.token, residenceId, uiState.value.periodFilter, uiState.value.customStartText, intent.endDate)
                }
            }
        }
    }

    private fun fetchDefaultPeriod() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val oneMonthAgo = today.minus(1, DateTimeUnit.MONTH)
        updateState {
            it.copy(
                customStartText = oneMonthAgo.toString(), // e.g., "2026-10-15"
                customEndText = today.toString()          // e.g., "2026-11-15"
            )
        }
    }

    fun updateActiveResidence(residence: ResidenceContext?) {
        updateState { it.copy(activeResidence = residence) }
        val session = sessionStorage?.loadSession()
        if (session != null && residence != null) {
            fetchData(session.token, residence.residenceId, uiState.value.periodFilter, uiState.value.customStartText, uiState.value.customEndText)
        }
    }

    private fun loadResidenceAndData(token: String, residenceId: String) {
        viewModelScope.launch {
            if (uiState.value.activeResidence == null || uiState.value.activeResidence?.residenceId != residenceId) {
                fetchResidencesUseCase(token).onSuccess { list ->
                    val matched = list.firstOrNull { it.residenceId == residenceId }
                    if (matched != null) {
                        updateState { it.copy(activeResidence = matched) }
                    }
                }
            }
            fetchData(token, residenceId, uiState.value.periodFilter, uiState.value.customStartText, uiState.value.customEndText)
        }
    }

    private fun fetchData(token: String, residenceId: String, periodFilter: String, startDate: String, endDate: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            getDashboardDataUseCase(token, residenceId, periodFilter, startDate, endDate)
                .onSuccess { data ->
                    updateState { it.copy(isLoading = false, dashboardData = data, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
