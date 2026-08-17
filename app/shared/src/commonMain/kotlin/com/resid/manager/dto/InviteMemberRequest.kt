package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class InviteMemberRequest(
    val email: String,
    val role: RoleDto // ADMIN, MANAGER, STAFF, TENANT
)
