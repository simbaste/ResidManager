package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class CurrencyCodeDto {
    XOF,
    EUR,
    USD
}

@Serializable
enum class CurrencySymbolDto(val label: String) {
    FRANC_CFA("FCFA"),
    EURO("Euro"),
    DOLLAR_US("US Dollar")
}
@Serializable
data class ResidenceSummaryItemDto(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String?,
    val totalUnits: Int,
    val currencySymbol: CurrencySymbolDto = CurrencySymbolDto.FRANC_CFA,
    val currencyCode: CurrencyCodeDto = CurrencyCodeDto.XOF,
    val kWhPrice: Double = 150.0
)

@Serializable
enum class RoleDto {
    OWNER,
    ADMIN,
    MANAGER,
    STAFF,
    TENANT
}

@Serializable
data class AssociatedResidenceItem(
    val id: String,
    val name: String,
    val address: String,
    val photoUrl: String?,
    val roleDto: RoleDto,
    val totalUnits: Int,
    val currencySymbol: CurrencySymbolDto = CurrencySymbolDto.FRANC_CFA,
    val currencyCode: CurrencyCodeDto = CurrencyCodeDto.XOF,
    val kWhPrice: Double = 150.0
)

@Serializable
data class ResidenceDirectoryDTO(
    val ownedResidences: List<ResidenceSummaryItemDto>,
    val associatedResidences: List<AssociatedResidenceItem>
)
