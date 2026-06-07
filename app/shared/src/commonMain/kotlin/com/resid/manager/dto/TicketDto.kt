package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}

@Serializable
enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
data class TicketDto(
    val id: String,
    val residenceId: String,
    val logementId: String?,
    val reporterId: String,
    val title: String,
    val description: String,
    val status: TicketStatus,
    val priority: TicketPriority,
    val interventionCost: Double?, // Becomes mandatory on CLOSED
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TicketCreateRequest(
    val residenceId: String,
    val logementId: String?,
    val reporterId: String,
    val title: String,
    val description: String,
    val priority: TicketPriority = TicketPriority.MEDIUM
)

@Serializable
data class TicketUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val status: TicketStatus? = null,
    val priority: TicketPriority? = null,
    val interventionCost: Double? = null
) {
    init {
        // Enforce business rule: Closing a ticket requires a valid intervention cost
        if (status == TicketStatus.CLOSED) {
            require(interventionCost != null && interventionCost >= 0.0) {
                "Validation Error: Closing a ticket requires an intervention cost greater than or equal to 0."
            }
        }
    }
}
