package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResidenceDto(
    val id: String,
    val name: String,
    val address: String,
    val defaultCurrency: String = "XOF", // Default pivot currency: FCFA (XOF)
    val kWhPrice: Double,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ResidenceCreateRequest(
    val name: String,
    val address: String,
    val defaultCurrency: String = "XOF",
    val kWhPrice: Double
)

@Serializable
data class ResidenceUpdateRequest(
    val name: String? = null,
    val address: String? = null,
    val defaultCurrency: String? = null,
    val kWhPrice: Double? = null
)

@Serializable
data class ResidenceMemberDto(
    val id: String,
    val userId: String,
    val residenceId: String,
    val role: UserRole,
    val createdAt: String
)

@Serializable
data class ResidenceMemberCreateRequest(
    val userId: String,
    val residenceId: String,
    val role: UserRole
)
