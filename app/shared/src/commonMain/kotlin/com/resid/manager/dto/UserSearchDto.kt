package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSearchDto(
    val id: String,
    val name: String,
    val email: String,
)