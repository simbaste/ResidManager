package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionTypeDto {
    INCOME, EXPENSE
}

@Serializable
enum class TransactionStatusDto {
    UNPAID,
    PAID,
    CANCELLED
}

@Serializable
data class TransactionDto(
    val id: String,
    val residenceId: String,
    val logementId: String?,
    val leaseId: String?,
    val type: TransactionTypeDto,
    val amount: Double,
    val description: String,
    val transactionDate: String,
    val status: TransactionStatusDto,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TransactionCreateRequest(
    val residenceId: String,
    val logementId: String?,
    val leaseId: String?,
    val type: TransactionTypeDto,
    val amount: Double,
    val description: String,
    val transactionDate: String,
    val status: TransactionStatusDto = TransactionStatusDto.UNPAID
)
