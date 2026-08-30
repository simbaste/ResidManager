package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TicketStatusDto {
    OPEN,
    IN_PROGRESS,
    CLOSED
}

@Serializable
data class TicketCategoryRequest(
    val key: String,
    val label: String
)

@Serializable
data class TicketCategoryDto(
    val id: String,
    val key: String,
    val label: String,
    val residenceId: String? = null
)

@Serializable
enum class TicketUrgencyDto {
    LOW,
    MEDIUM,
    CRITICAL
}

@Serializable
data class TicketDto(
    val id: String,
    val unitId: String,
    val creatorId: String,
    val category: TicketCategoryDto,
    val title: String,
    val description: String,
    val urgency: TicketUrgencyDto,
    val status: TicketStatusDto,
    val interventionCost: Double,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TicketCreateRequest(
    val unitId: String,
    val categoryId: String, // foreign key id of selected category
    val title: String,
    val description: String,
    val urgency: TicketUrgencyDto
)

@Serializable
data class TicketUpdateRequest(
    val status: TicketStatusDto? = null,
    val interventionCost: Double? = null,
    val comment: String? = null
)
