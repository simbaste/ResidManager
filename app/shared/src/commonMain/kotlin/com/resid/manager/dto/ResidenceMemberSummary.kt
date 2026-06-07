package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResidenceMemberSummary(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val role: String, // OWNER, ADMIN, MANAGER, STAFF, TENANT
    val status: String // PENDING_APPROVAL, INVITED, ACCEPTED
)
