package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(
    val residenceId: String,
    val userId: String,
    val role: RoleDto,
    val status: InvitationStatusDto
)
