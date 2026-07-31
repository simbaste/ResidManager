package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentDto(
    val id: String,
    val key: String,
    val label: String
)

@Serializable
enum class UnitStatusDto {
    AVAILABLE, OCCUPIED, RESERVED
}

@Serializable
data class UnitDto(
    val id: String,
    val residenceId: String,
    val name: String,
    val floor: String,
    val type: String,
    val nominalRent: Double,
    val serviceCharges: Double,
    val initialElectricityIndex: Double,
    val status: UnitStatusDto,
    val equipments: List<EquipmentDto> = emptyList()
)

@Serializable
data class LogementCreateRequest(
    val name: String,
    val floor: String,
    val type: String,
    val nominalRent: Double,
    val serviceCharges: Double,
    val initialElectricityIndex: Double,
    val equipementIds: List<String> = emptyList()
)
