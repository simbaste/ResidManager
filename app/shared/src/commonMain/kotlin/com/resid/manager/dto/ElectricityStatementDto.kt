package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class StatementStatus {
    UNPAID,
    PAID
}

@Serializable
data class ElectricityStatementDto(
    val id: String,
    val logementId: String,
    val previousIndex: Double,
    val newIndex: Double,
    val amountDue: Double, // (newIndex - previousIndex) * kWhPrice
    val statementDate: String,
    val status: StatementStatus,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ElectricityStatementCreateRequest(
    val logementId: String,
    val previousIndex: Double,
    val newIndex: Double,
    val statementDate: String
) {
    init {
        // Enforce business rule: New Index MUST be >= Previous Index
        require(newIndex >= previousIndex) {
            "Validation Error: New Index ($newIndex) cannot be less than Previous Index ($previousIndex)."
        }
    }
}

@Serializable
data class ElectricityStatementUpdateRequest(
    val status: StatementStatus? = null
)
