package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class LeaseStatus {
    PENDING_PAYMENT,
    DOWN_PAYMENT_PAID,
    PENDING_SIGNATURE,
    SIGNED_ACTIVE
}

@Serializable
data class LeaseDto(
    val id: String,
    val logementId: String,
    val tenantId: String,
    val startDate: String, // ISO-8601 Date
    val endDate: String,   // ISO-8601 Date
    val depositAmount: Double,
    val monthlyRentAtSign: Double,
    val status: LeaseStatus,
    val createdAt: String,
    val updatedAt: String
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
    val logementId: String,
    val depositAmount: Double,
    val paymentFrequency: String, // "MONTHLY", "ANNUAL"
    val startDate: String, // ISO-8601 Date String
    val endDate: String,   // ISO-8601 Date String
    val monthlyRentAtSign: Double,
    val advanceMonths: Int? = null,
    val advancePaymentAmount: Double? = null
)

@Serializable
data class LeaseUpdateRequest(
    val startDate: String? = null,
    val endDate: String? = null,
    val depositAmount: Double? = null,
    val monthlyRentAtSign: Double? = null,
    val status: LeaseStatus? = null
)

@Serializable
data class LeasePaymentRequest(
    val amountPaid: Double
)
