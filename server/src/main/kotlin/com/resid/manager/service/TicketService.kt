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
        categoryId: UUID,
        title: String,
        description: String,
        urgency: TicketUrgencyDto
    ): TicketDto {
        // 1. Role validation
        val dbLogement = Logement.findById(logementId) ?: throw Exception("Logement introuvable.")
        val dbUser = User.findById(creatorId) ?: throw Exception("Utilisateur introuvable.")
        val dbCategory = TicketCategoryEntity.findById(categoryId) ?: throw Exception("Catégorie de ticket introuvable.")
        
        // Find role in residence members
        val memberRole = ResidenceMembers
            .select(ResidenceMembers.roleDto)
            .where { 
                (ResidenceMembers.userId eq creatorId) and 
                (ResidenceMembers.residenceId eq dbLogement.residence.id.value) 
            }
            .map { it[ResidenceMembers.roleDto] }
            .firstOrNull() ?: throw Exception("Accès interdit : vous ne faites pas partie de cette résidence.")

        val isAuthorized = when (memberRole.name.uppercase()) {
            "OWNER", "ADMIN", "MANAGER", "STAFF" -> true
            "TENANT" -> {
                // Tenant is only allowed if they have an active lease on this logement
                val activeLeaseCount = Baux
                    .select(Baux.id)
                    .where {
                        (Baux.logementId eq logementId) and 
                        (Baux.tenantId eq creatorId) and 
                        (Baux.status eq LeaseStatus.SIGNED_ACTIVE)
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
            this.category = dbCategory
            this.title = title
            this.description = description
            this.urgency = urgency.convert()
            this.status = TicketStatus.OPEN
            this.interventionCost = 0.0
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
        ticket.flush()

        return TicketDto(
            id = ticket.id.value.toString(),
            logementId = ticket.logement.id.value.toString(),
            creatorId = ticket.creator.id.value.toString(),
            category = TicketCategoryDto(
                id = ticket.category.id.value.toString(),
                key = ticket.category.key,
                label = ticket.category.label,
                residenceId = ticket.category.residence?.id?.value?.toString()
            ),
            title = ticket.title,
            description = ticket.description,
            urgency = urgency,
            status = TicketStatusDto.OPEN,
            interventionCost = ticket.interventionCost,
            createdAt = ticket.createdAt.toString(),
            updatedAt = ticket.updatedAt.toString()
        )
    }

    fun updateTicketStatus(
        ticketId: UUID,
        newStatus: TicketStatusDto,
        cost: Double?,
        comment: String?,
        updaterUserId: UUID
    ): TicketDto {
        val ticket = Ticket.findById(ticketId) ?: throw Exception("Ticket de maintenance introuvable.")
        val previousStatus = ticket.status.convert()

        // Validate state transition flow: OPEN -> IN_PROGRESS -> CLOSED
        if (newStatus == TicketStatusDto.IN_PROGRESS && previousStatus != TicketStatusDto.OPEN) {
            throw IllegalArgumentException("Transition impossible : un ticket ne peut passer à IN_PROGRESS que s'il est au statut OPEN.")
        }
        if (newStatus == TicketStatusDto.CLOSED && previousStatus != TicketStatusDto.IN_PROGRESS) {
            throw IllegalArgumentException("Transition impossible : un ticket ne peut passer à CLOSED que s'il est au statut IN_PROGRESS.")
        }

        // Role check for transition to CLOSED
        val memberRole = ResidenceMembers
            .select(ResidenceMembers.roleDto)
            .where { 
                (ResidenceMembers.userId eq updaterUserId) and 
                (ResidenceMembers.residenceId eq ticket.logement.residence.id.value) 
            }
            .map { it[ResidenceMembers.roleDto] }
            .firstOrNull() ?: throw Exception("Accès interdit : membre introuvable.")

        if (newStatus == TicketStatusDto.CLOSED) {
            val isAllowedToClose = when (memberRole) {
                Role.MANAGER, Role.ADMIN, Role.OWNER -> true
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
                this.type = TransactionType.EXPENSE
                this.category = TransactionCategory.MAINTENANCE
                this.amount = cost
                this.description = "Frais de maintenance - Ticket #${ticket.title} (Status: ${newStatus.name}) " + 
                    if (!comment.isNullOrBlank()) "- $comment" else ""
                this.relatedEntityType = EntityType.TICKET
                this.relatedEntityId = ticket.id.value
                this.transactionDate = LocalDate.now()
                this.createdAt = LocalDateTime.now()
                this.updatedAt = LocalDateTime.now()
            }
        }

        ticket.status = newStatus.convert()
        ticket.updatedAt = LocalDateTime.now()
        ticket.flush()

        return TicketDto(
            id = ticket.id.value.toString(),
            logementId = ticket.logement.id.value.toString(),
            creatorId = ticket.creator.id.value.toString(),
            category = TicketCategoryDto(
                id = ticket.category.id.value.toString(),
                key = ticket.category.key,
                label = ticket.category.label,
                residenceId = ticket.category.residence?.id?.value?.toString()
            ),
            title = ticket.title,
            description = ticket.description,
            urgency = ticket.urgency.convert(),
            status = newStatus,
            interventionCost = ticket.interventionCost,
            createdAt = ticket.createdAt.toString(),
            updatedAt = ticket.updatedAt.toString()
        )
    }
}
