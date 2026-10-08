package com.resid.manager.dto

import kotlinx.serialization.Serializable


@Serializable
data class InviteUpdateRequest(
    val status: InvitationStatusDto,
    val reason: String? = null
)

