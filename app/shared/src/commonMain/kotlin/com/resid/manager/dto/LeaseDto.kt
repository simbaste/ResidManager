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
data class LeaseCreateRequest(
    val logementId: String,
    val tenantId: String,
    val startDate: String,
    val endDate: String,
    val depositAmount: Double,
    val monthlyRentAtSign: Double
)

@Serializable
data class LeaseUpdateRequest(
    val startDate: String? = null,
    val endDate: String? = null,
    val depositAmount: Double? = null,
    val monthlyRentAtSign: Double? = null,
    val status: LeaseStatus? = null
)
