package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResidenceSummaryItem(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String?,
    val totalUnits: Int,
    val currencySymbol: String = "FCFA",
    val currencyCode: String = "XOF"
)

@Serializable
data class AssociatedResidenceItem(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String?,
    val role: String, // ADMIN, MANAGER, STAFF, TENANT
    val totalUnits: Int,
    val currencySymbol: String = "FCFA",
    val currencyCode: String = "XOF"
)

@Serializable
data class ResidenceDirectoryDTO(
    val ownedResidences: List<ResidenceSummaryItem>,
    val associatedResidences: List<AssociatedResidenceItem>
)
