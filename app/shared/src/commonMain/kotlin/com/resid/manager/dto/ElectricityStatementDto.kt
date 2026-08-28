package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class ElectricityStatusDto {
    UNPAID,
    PAID
}

@Serializable
data class ElectricityStatementDto(
    val id: String,
    val unitId: String,
    val previousIndex: Double,
    val newIndex: Double,
    val kWhPriceApplied: Double,
    val amountDue: Double, // (newIndex - previousIndex) * kWhPriceApplied
    val statementDate: String,
    val status: ElectricityStatusDto,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ElectricityStatementCreateRequest(
    val unitId: String,
    val previousIndex: Double,
    val newIndex: Double,
    val kWhPriceApplied: Double,
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
    val previousIndex: Double? = null,
    val newIndex: Double? = null,
    val kWhPriceApplied: Double? = null,
    val statementDate: String? = null,
    val status: ElectricityStatusDto? = null
)

@Serializable
data class ElectricityStatusUpdateRequest(
    val status: ElectricityStatusDto
)

