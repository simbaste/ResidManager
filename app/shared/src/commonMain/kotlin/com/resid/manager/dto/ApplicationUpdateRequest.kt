package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationUpdateRequest(
    val residenceId: String,
    val userId: String,
    val status: ApplicationStatusDto,
    val role: RoleDto,
)