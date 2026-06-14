package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class FinanceTransactionDto(
    val id: String,
    val residenceId: String,
    val type: String, // "INCOME", "EXPENSE"
    val category: String, // "Cleaning", "Fuel", "Security", "Maintenance", "Taxes", "Other"
    val amount: Double,
    val description: String,
    val relatedEntityType: String?,
    val relatedEntityId: String?,
    val transactionDate: String,
    val createdAt: String
)

@Serializable
data class ExpenseRecordRequest(
    val category: String,
    val amount: Double,
    val description: String,
    val transactionDate: String
)
