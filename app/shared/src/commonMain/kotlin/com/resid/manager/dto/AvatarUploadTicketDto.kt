package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvatarUploadTicketRequest(
    val mimeType: String,
    val extension: String
)

@Serializable
data class AvatarUploadTicketResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val expiresInSeconds: Long = 300
)
