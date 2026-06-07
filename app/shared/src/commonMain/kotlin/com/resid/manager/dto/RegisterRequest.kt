package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val birthDate: String? = null,
    val phone: String? = null,
    val email: String,
    val passwordPlain: String
)
