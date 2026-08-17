package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class ApplicationStatusDto {
    PENDING, APPROVED, REJECTED
}

@Serializable
data class ResidenceApplicationDto(
    val residenceId: String,
    val residenceName: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val roleDto: RoleDto, // OWNER, ADMIN, MANAGER, STAFF, TENANT
    val status: ApplicationStatusDto,
    val createdAt: String,
    val updatedAt: String,
)
