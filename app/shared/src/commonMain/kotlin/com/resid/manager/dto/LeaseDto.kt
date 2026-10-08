package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class LeaseStatusDto {
    PENDING_PAYMENT,
    DOWN_PAYMENT_PAID,
    PARTIALLY_PAID,
    PENDING_SIGNATURE,
    SIGNED_ACTIVE,
    TERMINATED
}

@Serializable
enum class LeaseCategory {
    DEPOSIT,
    RENT
}

@Serializable
data class LeasePaymentDto(
    val id: String,
    val category: LeaseCategory,
    val amount: Double,
    val description: String,
    val transactionDate: String
)

@Serializable
enum class PaymentFrequencyDto {
    MONTHLY, ANNUAL
}

@Serializable
data class LeaseDto(
    val id: String,
    val residenceUnitId: String,
    val tenantId: String,
    val startDate: String, // ISO-8601 Date
    val endDate: String,   // ISO-8601 Date
    val depositAmount: Double,
    val monthlyRentAtSign: Double,
    val status: LeaseStatusDto,
    val createdAt: String,
    val updatedAt: String,
    val paymentFrequencyDto: PaymentFrequencyDto = PaymentFrequencyDto.MONTHLY,
    val advanceMonths: Int = 1,
    val payments: List<LeasePaymentDto> = emptyList()
)

@Serializable
data class InlineTenantCreateRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null
)

@Serializable
data class LeaseCreateRequest(
    val tenantId: String?, // Set if choosing existing (UUID as String)
    val inlineTenant: InlineTenantCreateRequest?, // Set if creating a new one inline
    @Deprecated("This field can be remove when using /api/leases route")
    val residenceUnitId: String,
    val depositAmount: Double,
    val paymentFrequency: String, // "MONTHLY", "ANNUAL"
    val startDate: String, // ISO-8601 Date String
    val endDate: String,   // ISO-8601 Date String
    val monthlyRentAtSign: Double, //
    val advanceMonths: Int? = null,
    val advancePaymentAmount: Double? = null
)

@Serializable
data class LeaseUpdateRequest(
    val startDate: String? = null,
    val endDate: String? = null,
    val depositAmount: Double? = null,
    val monthlyRentAtSign: Double? = null,
    val advanceMonths: Int? = null,
    val paymentFrequency: String? = null,
    val status: LeaseStatusDto? = null
)

enum class TransactionCategoryDto {
    RENT, DEPOSIT, LEASE_PAYMENT, ELECTRICITY, MAINTENANCE, CLEANING, FUEL, SECURITY, TAXES, OTHER
}

@Serializable
data class LeasePaymentRequest(
    val amountPaid: Double,
    val category: TransactionCategoryDto
)
