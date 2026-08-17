package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationRequest(
    val residenceId: String,
    val userId: String,
    val role: RoleDto,
)