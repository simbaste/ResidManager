package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardReportDto(
    val residenceId: String,
    val totalRevenue: Double,         // RENT + ELECTRICITY in XOF
    val totalExpenses: Double,        // MAINTENANCE + OPERATIONAL EXPENSES in XOF
    val netProfit: Double,            // Revenue - Expenses in XOF
    val occupancyRate: Double,        // (Occupied units / Total units)
    val activeLeasesCount: Int,
    val openTicketsCount: Int,
    val pendingUnpaidRentsCount: Int,
    val unpaidRentsAmount: Double
)
