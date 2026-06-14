package com.resid.manager.service

import com.resid.manager.data.*
import com.resid.manager.dto.*
import org.jetbrains.exposed.sql.and
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TicketService {

    fun createTicket(
        logementId: UUID,
        creatorId: UUID,
        category: TicketCategory,
        title: String,
        description: String,
        urgency: TicketUrgency
    ): TicketDto {
        // 1. Role validation
        val dbLogement = Logement.findById(logementId) ?: throw Exception("Logement introuvable.")
        val dbUser = User.findById(creatorId) ?: throw Exception("Utilisateur introuvable.")
        
        // Find role in residence members
        val memberRole = ResidenceMembers
            .select(ResidenceMembers.role)
            .where { 
                (ResidenceMembers.userId eq creatorId) and 
                (ResidenceMembers.residenceId eq dbLogement.residence.id.value) 
            }
            .map { it[ResidenceMembers.role] }
            .firstOrNull() ?: throw Exception("Accès interdit : vous ne faites pas partie de cette résidence.")

        val isAuthorized = when (memberRole.uppercase()) {
            "OWNER", "ADMIN", "MANAGER", "STAFF" -> true
            "TENANT" -> {
                // Tenant is only allowed if they have an active lease on this logement
                val activeLeaseCount = Baux
                    .select(Baux.id)
                    .where {
                        (Baux.logementId eq logementId) and 
                        (Baux.tenantId eq creatorId) and 
                        (Baux.status eq "SIGNED_ACTIVE")
                    }
                    .count()
                activeLeaseCount > 0
            }
            else -> false
        }

        if (!isAuthorized) {
            throw IllegalArgumentException("Droit de création refusé : vous devez posséder un bail actif sur cette unité.")
        }

        // 2. Creation
        val ticket = Ticket.new {
            this.logement = dbLogement
            this.creator = dbUser
            this.category = category.name
            this.title = title
            this.description = description
            this.urgency = urgency.name
            this.status = "OPEN"
            this.interventionCost = 0.0
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
        ticket.flush()

        return TicketDto(
            id = ticket.id.value.toString(),
            logementId = ticket.logement.id.value.toString(),
            creatorId = ticket.creator.id.value.toString(),
            category = category,
            title = ticket.title,
            description = ticket.description,
            urgency = urgency,
            status = TicketStatus.OPEN,
            interventionCost = ticket.interventionCost,
            createdAt = ticket.createdAt.toString(),
            updatedAt = ticket.updatedAt.toString()
        )
    }

    fun updateTicketStatus(
        ticketId: UUID,
        newStatus: TicketStatus,
        cost: Double?,
        comment: String?,
        updaterUserId: UUID
    ): TicketDto {
        val ticket = Ticket.findById(ticketId) ?: throw Exception("Ticket de maintenance introuvable.")
        val previousStatus = TicketStatus.valueOf(ticket.status)

        // Validate state transition flow: OPEN -> IN_PROGRESS -> CLOSED
        if (newStatus == TicketStatus.IN_PROGRESS && previousStatus != TicketStatus.OPEN) {
            throw IllegalArgumentException("Transition impossible : un ticket ne peut passer à IN_PROGRESS que s'il est au statut OPEN.")
        }
        if (newStatus == TicketStatus.CLOSED && previousStatus != TicketStatus.IN_PROGRESS) {
            throw IllegalArgumentException("Transition impossible : un ticket ne peut passer à CLOSED que s'il est au statut IN_PROGRESS.")
        }

        // Role check for transition to CLOSED
        val memberRole = ResidenceMembers
            .select(ResidenceMembers.role)
            .where { 
                (ResidenceMembers.userId eq updaterUserId) and 
                (ResidenceMembers.residenceId eq ticket.logement.residence.id.value) 
            }
            .map { it[ResidenceMembers.role] }
            .firstOrNull() ?: throw Exception("Accès interdit : membre introuvable.")

        if (newStatus == TicketStatus.CLOSED) {
            val isAllowedToClose = when (memberRole.uppercase()) {
                "MANAGER", "ADMIN", "OWNER" -> true
                else -> false
            }

            if (!isAllowedToClose) {
                throw IllegalArgumentException("Action interdite : seuls les MANAGER, ADMIN ou OWNER peuvent clôturer un ticket.")
            }
        }

        // Append optional comment
        if (!comment.isNullOrBlank()) {
            ticket.description = ticket.description + "\n\n[Suivi - ${newStatus.name}] : $comment"
        }

        // Process optional cost and generate Financial Transaction Expense if cost > 0
        if (cost != null && cost > 0.0) {
            ticket.interventionCost = ticket.interventionCost + cost

            FinancialTransaction.new {
                this.residence = ticket.logement.residence
                this.type = "EXPENSE"
                this.category = "Maintenance"
                this.amount = cost
                this.description = "Frais de maintenance - Ticket #${ticket.title} (Status: ${newStatus.name}) " + 
                    if (!comment.isNullOrBlank()) "- $comment" else ""
                this.relatedEntityType = "TICKET"
                this.relatedEntityId = ticket.id.value
                this.transactionDate = LocalDate.now()
                this.createdAt = LocalDateTime.now()
                this.updatedAt = LocalDateTime.now()
            }
        }

        ticket.status = newStatus.name
        ticket.updatedAt = LocalDateTime.now()
        ticket.flush()

        return TicketDto(
            id = ticket.id.value.toString(),
            logementId = ticket.logement.id.value.toString(),
            creatorId = ticket.creator.id.value.toString(),
            category = TicketCategory.valueOf(ticket.category),
            title = ticket.title,
            description = ticket.description,
            urgency = TicketUrgency.valueOf(ticket.urgency),
            status = newStatus,
            interventionCost = ticket.interventionCost,
            createdAt = ticket.createdAt.toString(),
            updatedAt = ticket.updatedAt.toString()
        )
    }
}
