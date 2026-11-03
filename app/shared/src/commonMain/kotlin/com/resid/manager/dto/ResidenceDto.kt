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
    val currency: CurrencyCodeDto,
    val kWhPrice: Double,
    val photoUrl: String? = null
)

@Serializable
data class ResidenceUpdateRequest(
    val name: String? = null,
    val address: String? = null,
    val defaultCurrency: String? = null,
    val kWhPrice: Double? = null,
    val photoUrl: String? = null
)

@Serializable
data class ResidencePhotoUpdateRequest(
    val photoUrl: String
)

@Serializable
data class ResidencePhotoDto(
    val url: String,
    val description: String = ""
)

@Serializable
data class ResidencePhotosUpdateRequest(
    val photos: List<ResidencePhotoDto>
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
