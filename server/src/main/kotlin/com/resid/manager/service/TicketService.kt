package com.resid.manager.service

import com.resid.manager.data.EntityType
import com.resid.manager.data.FinancialTransaction
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.LeaseStatus
import com.resid.manager.data.Leases
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.ResidenceUnit
import com.resid.manager.data.ResidenceUnits
import com.resid.manager.data.Role
import com.resid.manager.data.Ticket
import com.resid.manager.data.TicketCategories
import com.resid.manager.data.TicketCategoryEntity
import com.resid.manager.data.TicketStatus
import com.resid.manager.data.Tickets
import com.resid.manager.data.TransactionCategory
import com.resid.manager.data.TransactionType
import com.resid.manager.data.User
import com.resid.manager.data.convert
import com.resid.manager.data.isManager
import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.TicketCategoryRequest
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketDto
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.TicketUpdateRequest
import com.resid.manager.dto.TicketUrgencyDto
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TicketService {

    suspend fun isResidenceManager(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isManager() }
    }

    suspend fun isResidenceMember(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.userId)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.count() > 0
    }

    // -------------------------------------------------------------
    // Ticket Categories Management
    // -------------------------------------------------------------
    suspend fun getCategories(residenceId: UUID?): List<TicketCategoryDto> = dbQuery {
        val conditions: Op<Boolean> = if (residenceId != null) {
            (TicketCategories.residenceId.isNull()) or (TicketCategories.residenceId eq residenceId)
        } else {
            TicketCategories.residenceId.isNull()
        }

        TicketCategoryEntity.find { conditions }.map { it.toDto() }
    }

    suspend fun createCategory(
        requesterId: UUID,
        residenceId: UUID,
        request: TicketCategoryRequest
    ): TicketCategoryDto = dbQuery {
        val dbResidence = Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires et administrateurs peuvent créer des catégories de ticket."
            )
        }

        val keyUpper = request.key.trim().uppercase()
        val exists = TicketCategoryEntity.find { TicketCategories.key eq keyUpper }.count() > 0
        if (exists) {
            throw HttpError(HttpStatusCode.Conflict, "Une catégorie avec cette clé existe déjà : $keyUpper")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        val entity = TicketCategoryEntity.new {
            this.residence = dbResidence
            this.key = keyUpper
            this.label = request.label
            this.createdAt = now
            this.updatedAt = now
        }
        entity.flush()
        entity.toDto()
    }

    suspend fun updateCategory(
        requesterId: UUID,
        categoryId: UUID,
        request: TicketCategoryRequest
    ): TicketCategoryDto = dbQuery {
        val entity = TicketCategoryEntity.findById(categoryId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Catégorie de ticket introuvable.")

        val residenceId = entity.residence?.id?.value
        if (residenceId != null && !isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires de cette résidence peuvent modifier cette catégorie."
            )
        }

        val keyUpper = request.key.trim().uppercase()
        val exists = TicketCategoryEntity.find { TicketCategories.key eq keyUpper }.count() > 0
        if (exists) {
            throw HttpError(HttpStatusCode.Conflict, "Une catégorie avec cette clé existe déjà : $keyUpper")
        }

        entity.label = request.label
        entity.updatedAt = LocalDateTime.now(Clock.systemUTC())
        entity.flush()
        entity.toDto()
    }

    suspend fun deleteCategory(
        requesterId: UUID,
        categoryId: UUID
    ) = dbQuery {
        val entity = TicketCategoryEntity.findById(categoryId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Catégorie de ticket introuvable.")

        val residenceId = entity.residence?.id?.value
            ?: throw HttpError(
                HttpStatusCode.Forbidden,
                "Action interdite : Les catégories globales par défaut ne peuvent pas être supprimées."
            )

        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires peuvent supprimer cette catégorie."
            )
        }

        entity.delete()
    }

    // -------------------------------------------------------------
    // Tickets Management
    // -------------------------------------------------------------
    suspend fun createTicket(
        creatorId: UUID,
        request: TicketCreateRequest
    ): TicketDto = dbQuery {
        val unitUuid = try {
            UUID.fromString(request.unitId)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "ID de logement invalide : ${e.message}")
        }

        val categoryUuid = try {
            UUID.fromString(request.categoryId)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "ID de catégorie invalide : ${e.message}")
        }

        val dbResidenceUnit = ResidenceUnit.findById(unitUuid)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

        val dbUser = User.findById(creatorId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable.")

        val dbCategory = TicketCategoryEntity.findById(categoryUuid)
            ?: throw HttpError(HttpStatusCode.NotFound, "Catégorie de ticket introuvable.")

        val categoryResidenceId = dbCategory.residence?.id?.value
        if (categoryResidenceId != null && categoryResidenceId != dbResidenceUnit.residence.id.value) {
            throw HttpError(HttpStatusCode.BadRequest, "La catégorie sélectionnée ne fait pas partie de cette résidence.")
        }

        val residenceId = dbResidenceUnit.residence.id.value

        // Member role check
        val memberRole = ResidenceMembers
            .select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq creatorId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }
            .map { it[ResidenceMembers.role] }
            .firstOrNull() ?: throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous ne faites pas partie de cette résidence."
            )

        val isAuthorized = when (memberRole) {
            Role.OWNER, Role.ADMIN, Role.MANAGER, Role.STAFF -> true
            Role.TENANT -> {
                // Tenant can only create tickets on their active lease unit
                val activeLeaseCount = Leases
                    .select(Leases.id)
                    .where {
                        (Leases.residenceUnitId eq unitUuid) and
                        (Leases.tenantId eq creatorId) and
                        (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
                    }
                    .count()
                activeLeaseCount > 0
            }
        }

        if (!isAuthorized) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Droit de création refusé : vous devez posséder un bail actif sur cette unité pour ouvrir un ticket."
            )
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        val ticket = Ticket.new {
            this.residenceUnit = dbResidenceUnit
            this.creator = dbUser
            this.category = dbCategory
            this.title = request.title
            this.description = request.description
            this.urgency = request.urgency.convert()
            this.status = TicketStatus.OPEN
            this.interventionCost = 0.0
            this.createdAt = now
            this.updatedAt = now
        }
        ticket.flush()
        ticket.toDto()
    }

    suspend fun updateTicketStatus(
        updaterUserId: UUID,
        ticketId: UUID,
        request: TicketUpdateRequest
    ): TicketDto = dbQuery {
        val ticket = Ticket.findById(ticketId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Ticket de maintenance introuvable.")

        val newStatus = request.status
            ?: throw HttpError(HttpStatusCode.BadRequest, "Nouveau statut de ticket manquant.")

        val previousStatus = ticket.status.convert()

        // Validate state transitions: OPEN -> IN_PROGRESS -> CLOSED
        if (newStatus == TicketStatusDto.IN_PROGRESS && previousStatus != TicketStatusDto.OPEN) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Transition impossible : un ticket ne peut passer à IN_PROGRESS que s'il est au statut OPEN."
            )
        }
        if (newStatus == TicketStatusDto.CLOSED && previousStatus != TicketStatusDto.IN_PROGRESS) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Transition impossible : un ticket ne peut passer à CLOSED que s'il est au statut IN_PROGRESS."
            )
        }

        val residenceId = ticket.residenceUnit.residence.id.value
        val memberRole = ResidenceMembers
            .select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq updaterUserId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }
            .map { it[ResidenceMembers.role] }
            .firstOrNull() ?: throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous ne faites pas partie de cette résidence."
            )

        if (newStatus == TicketStatusDto.CLOSED && !memberRole.isManager()) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Action interdite : Seuls les gestionnaires et administrateurs peuvent clôturer un ticket."
            )
        }

        val comment = request.comment
        if (!comment.isNullOrBlank()) {
            ticket.description += "\n\n[Suivi - ${newStatus.name}] : $comment"
        }

        val cost = request.interventionCost
        val now = LocalDateTime.now(Clock.systemUTC())

        if (cost != null && cost > 0.0) {
            ticket.interventionCost += cost

            FinancialTransaction.new {
                this.residence = ticket.residenceUnit.residence
                this.type = TransactionType.EXPENSE
                this.category = TransactionCategory.MAINTENANCE
                this.amount = cost
                this.description = "Frais de maintenance - Ticket #${ticket.title} (Status: ${newStatus.name})" +
                    if (!comment.isNullOrBlank()) " - $comment" else ""
                this.relatedEntityType = EntityType.TICKET
                this.relatedEntityId = ticket.id.value
                this.transactionDate = LocalDate.now(Clock.systemUTC())
                this.createdAt = now
                this.updatedAt = now
            }
        }

        ticket.status = newStatus.convert()
        ticket.updatedAt = now
        ticket.flush()
        ticket.toDto()
    }

    suspend fun getTickets(
        requesterId: UUID,
        residenceId: UUID?,
        unitId: UUID?,
        status: TicketStatusDto?,
        urgency: TicketUrgencyDto?,
        creatorId: UUID?
    ): List<TicketDto> = dbQuery {
        var conditions: Op<Boolean> = Op.TRUE

        if (residenceId != null) {
            if (!isResidenceMember(requesterId, residenceId)) {
                throw HttpError(
                    HttpStatusCode.Forbidden,
                    "Accès interdit : Vous ne faites pas partie de cette résidence."
                )
            }
            conditions = conditions and (ResidenceUnits.residenceId eq residenceId)
        }

        if (unitId != null) {
            conditions = conditions and (Tickets.residenceUnitId eq unitId)
        }

        if (status != null) {
            conditions = conditions and (Tickets.status eq status.convert())
        }

        if (urgency != null) {
            conditions = conditions and (Tickets.urgency eq urgency.convert())
        }

        if (creatorId != null) {
            conditions = conditions and (Tickets.creatorId eq creatorId)
        }

        (Tickets innerJoin ResidenceUnits)
            .select(Tickets.columns)
            .where { conditions }
            .orderBy(Tickets.createdAt to SortOrder.DESC)
            .let { Ticket.wrapRows(it).toList() }
            .map { it.toDto() }
    }

    suspend fun getTicketById(
        requesterId: UUID,
        ticketId: UUID
    ): TicketDto = dbQuery {
        val ticket = Ticket.findById(ticketId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Ticket introuvable.")

        val residenceId = ticket.residenceUnit.residence.id.value
        val isCreator = ticket.creator.id.value == requesterId

        if (!isCreator && !isResidenceMember(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous n'avez pas accès à ce ticket."
            )
        }

        ticket.toDto()
    }

    suspend fun deleteTicket(
        requesterId: UUID,
        ticketId: UUID
    ) = dbQuery {
        val ticket = Ticket.findById(ticketId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Ticket introuvable.")

        val residenceId = ticket.residenceUnit.residence.id.value
        val isCreator = ticket.creator.id.value == requesterId
        val isManager = isResidenceManager(requesterId, residenceId)

        if (!isCreator && !isManager) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous ne pouvez pas supprimer ce ticket."
            )
        }

        ticket.delete()
    }

    private fun TicketCategoryEntity.toDto(): TicketCategoryDto {
        return TicketCategoryDto(
            id = this.id.value.toString(),
            key = this.key,
            label = this.label,
            residenceId = this.residence?.id?.value?.toString()
        )
    }

    private fun Ticket.toDto(): TicketDto {
        return TicketDto(
            id = this.id.value.toString(),
            unitId = this.residenceUnit.id.value.toString(),
            creatorId = this.creator.id.value.toString(),
            category = this.category.toDto(),
            title = this.title,
            description = this.description,
            urgency = this.urgency.convert(),
            status = this.status.convert(),
            interventionCost = this.interventionCost,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
}
