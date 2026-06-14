package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}

@Serializable
enum class TicketCategory {
    PLUMBING,
    ELECTRICITY,
    TILES,
    CEILING,
    OTHER
}

@Serializable
enum class TicketUrgency {
    LOW,
    MEDIUM,
    CRITICAL
}

@Serializable
data class TicketDto(
    val id: String,
    val logementId: String,
    val creatorId: String,
    val category: TicketCategory,
    val title: String,
    val description: String,
    val urgency: TicketUrgency,
    val status: TicketStatus,
    val interventionCost: Double,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TicketCreateRequest(
    val logementId: String,
    val category: TicketCategory,
    val title: String,
    val description: String,
    val urgency: TicketUrgency
)

@Serializable
data class TicketUpdateRequest(
    val status: TicketStatus? = null,
    val interventionCost: Double? = null,
    val comment: String? = null
)
