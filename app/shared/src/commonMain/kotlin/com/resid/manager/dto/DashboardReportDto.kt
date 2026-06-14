package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardDataDto(
    val residenceId: String,
    val totalRevenuesCollected: Double,
    val totalExpensesIncurred: Double,
    val netCashflow: Double,
    val delinquencyRate: Double,
    val occupancyRate: Double
)
