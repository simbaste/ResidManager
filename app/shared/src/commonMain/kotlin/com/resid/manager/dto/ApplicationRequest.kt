package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationRequest(
    val role: RoleDto,
)