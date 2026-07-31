package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    OWNER,
    ADMIN,
    MANAGER,
    STAFF,
    TENANT
}

fun UserRole.convert(): RoleDto {
    return when (this) {
        UserRole.OWNER -> RoleDto.OWNER
        UserRole.ADMIN -> RoleDto.ADMIN
        UserRole.MANAGER -> RoleDto.MANAGER
        UserRole.STAFF -> RoleDto.STAFF
        UserRole.TENANT -> RoleDto.TENANT
    }
}

fun RoleDto.convert(): UserRole {
    return when (this) {
        RoleDto.OWNER -> UserRole.OWNER
        RoleDto.ADMIN -> UserRole.ADMIN
        RoleDto.MANAGER -> UserRole.MANAGER
        RoleDto.STAFF -> UserRole.STAFF
        RoleDto.TENANT -> UserRole.TENANT
    }
}

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val phone: String?,
    val birthDate: String?,
    val createdAt: String, // ISO-8601 string
    val updatedAt: String  // ISO-8601 string
)

@Serializable
data class UserCreateRequest(
    val email: String,
    val passwordPlain: String,
    val name: String,
    val phone: String?,
)

@Serializable
data class UserUpdateRequest(
    val email: String? = null,
    val passwordPlain: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val phone: String? = null
)

@Serializable
data class AuthRequest(
    val email: String,
    val passwordPlain: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto
)
