package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    RENT_REVENUE,
    ELECTRICITY_REVENUE,
    OPERATIONAL_EXPENSE,
    MAINTENANCE_EXPENSE,
    OTHER
}

@Serializable
enum class TransactionStatus {
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
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val transactionDate: String,
    val status: TransactionStatus,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TransactionCreateRequest(
    val residenceId: String,
    val logementId: String?,
    val leaseId: String?,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val transactionDate: String,
    val status: TransactionStatus = TransactionStatus.UNPAID
)
