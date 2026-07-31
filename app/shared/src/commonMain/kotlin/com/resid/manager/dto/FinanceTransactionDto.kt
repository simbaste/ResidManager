package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class EntityTypeDto {
    BAIL, ELECTRICITY_STATEMENT, TICKET
}

@Serializable
data class FinanceTransactionDto(
    val id: String,
    val residenceId: String,
    val type: TransactionTypeDto, // "INCOME", "EXPENSE"
    val category: TransactionCategoryDto,
    val amount: Double,
    val description: String,
    val relatedEntityType: EntityTypeDto?,
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
