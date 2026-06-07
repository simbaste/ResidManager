package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResidenceContext(
    val residenceId: String,
    val residenceName: String,
    val userRoleInResidence: UserRole
)
