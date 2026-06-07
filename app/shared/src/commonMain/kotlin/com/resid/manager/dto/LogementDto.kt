package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogementDto(
    val id: String,
    val residenceId: String,
    val name: String,
    val floor: String,
    val type: String,
    val nominalRent: Double,
    val serviceCharges: Double,
    val initialElectricityIndex: Double,
    val status: String // "AVAILABLE", "OCCUPIED", "RESERVED"
)

@Serializable
data class LogementCreateRequest(
    val name: String,
    val floor: String,
    val type: String,
    val nominalRent: Double,
    val serviceCharges: Double,
    val initialElectricityIndex: Double
)
