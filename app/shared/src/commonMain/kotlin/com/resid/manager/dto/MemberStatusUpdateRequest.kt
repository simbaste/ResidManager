package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberStatusUpdateRequest(
    val status: String, // ACCEPTED, REJECTED
    val role: String? = null // optional role change
)
