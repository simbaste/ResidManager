package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class InvitationStatusDto {
    PENDING_APPROVAL, INVITED, ACCEPTED
}

@Serializable
data class ResidenceMemberSummaryDto(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val roleDto: RoleDto, // OWNER, ADMIN, MANAGER, STAFF, TENANT
    val status: InvitationStatusDto
)
