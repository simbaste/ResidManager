package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResidenceContext(
    val residenceId: String,
    val residenceName: String,
    val residenceAddress: String,
    val userRoleInResidence: UserRole,
    val totalUnits: Int = 0
)
