package com.resid.manager.service

import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.data.isAdmin
import com.resid.manager.dto.InvitationDto
import com.resid.manager.dto.InviteMemberRequest
import com.resid.manager.dto.InviteUpdateRequest
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

object InvitationService {

    suspend fun isResidenceAdmin(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isAdmin() }
    }

    suspend fun inviteMember(
        requesterUuid: UUID,
        residenceUuid: UUID,
        request: InviteMemberRequest
    ): Map<String, Any> = dbQuery {
        // Check if residence exists
        Residence.findById(residenceUuid) ?: throw HttpError(
            HttpStatusCode.NotFound,
            "Résidence introuvable."
        )

        // Check if requester is admin
        if (!isResidenceAdmin(requesterUuid, residenceUuid)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit: Seuls les propriétaires et administrateurs peuvent inviter des membres."
            )
        }

        // Find invited user by email
        val invitedUser = User.find { Users.email eq request.email }.firstOrNull()
            ?: throw HttpError(
                HttpStatusCode.NotFound,
                "Utilisateur introuvable avec l'adresse email : ${request.email}"
            )

        // Check if an invitation/membership already exists
        val existingMember = ResidenceMembers.select(ResidenceMembers.userId)
            .where {
                (ResidenceMembers.userId eq invitedUser.id.value) and
                (ResidenceMembers.residenceId eq residenceUuid)
            }.singleOrNull()

        if (existingMember != null) {
            throw HttpError(HttpStatusCode.Conflict, "Une invitation existe déjà pour cet utilisateur.")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        ResidenceMembers.insert {
            it[userId] = invitedUser.id.value
            it[residenceId] = residenceUuid
            it[role] = request.role.convert()
            it[status] = InvitationStatus.INVITED
            it[createdAt] = now
            it[updatedAt] = now
        }

        mapOf(
            "residenceId" to residenceUuid.toString(),
            "status" to "Invitation envoyée"
        )
    }

    suspend fun updateInvitation(
        requesterUuid: UUID,
        residenceUuid: UUID,
        request: InviteMemberRequest
    ): Map<String, String> = dbQuery {
        if (!isResidenceAdmin(requesterUuid, residenceUuid)) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
        }

        val invitedUser = User.find { Users.email eq request.email }.firstOrNull()
            ?: throw HttpError(
                HttpStatusCode.NotFound,
                "Utilisateur introuvable avec l'adresse email : ${request.email}"
            )

        val updated = ResidenceMembers.update({
            (ResidenceMembers.userId eq invitedUser.id.value) and
            (ResidenceMembers.residenceId eq residenceUuid)
        }) {
            it[role] = request.role.convert()
            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
        }

        if (updated == 0) throw HttpError(HttpStatusCode.NotFound, "Invitation non trouvée.")

        mapOf("status" to "Invitation mise à jour")
    }

    suspend fun processInvitation(
        requesterUuid: UUID,
        targetUserUuid: UUID,
        residenceUuid: UUID,
        request: InviteUpdateRequest
    ) = dbQuery {
        if (targetUserUuid != requesterUuid) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : vous ne pouvez modifier que votre propre invitation."
            )
        }

        Residence.findById(residenceUuid) ?: throw HttpError(
            HttpStatusCode.NotFound,
            "Résidence introuvable."
        )

        val memberRow = ResidenceMembers.select(ResidenceMembers.status)
            .where {
                (ResidenceMembers.userId eq targetUserUuid) and
                (ResidenceMembers.residenceId eq residenceUuid)
            }.singleOrNull() ?: throw HttpError(HttpStatusCode.NotFound, "Invitation introuvable.")

        val currentStatus = memberRow[ResidenceMembers.status]
        if (currentStatus == InvitationStatus.ACCEPTED) {
            throw HttpError(HttpStatusCode.NotAcceptable, "Cette invitation n'est plus en attente.")
        }

        val updated = ResidenceMembers.update({
            (ResidenceMembers.userId eq targetUserUuid) and
            (ResidenceMembers.residenceId eq residenceUuid)
        }) {
            it[status] = request.status.convert()
            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
        }

        if (updated == 0) throw HttpError(HttpStatusCode.NotFound, "Invitation introuvable.")
    }

    suspend fun getInvitations(
        requesterUuid: UUID,
        residenceUuid: UUID,
        queryUserUuid: UUID?,
        status: InvitationStatus?
    ): List<InvitationDto> = dbQuery {
        val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
        if (!isAdmin && queryUserUuid != requesterUuid) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
        }

        var conditions: Op<Boolean> = ResidenceMembers.residenceId eq residenceUuid
        if (queryUserUuid != null) {
            conditions = conditions and (ResidenceMembers.userId eq queryUserUuid)
        }
        if (status != null) {
            conditions = conditions and (ResidenceMembers.status eq status)
        }

        ResidenceMembers.selectAll()
            .where { conditions }
            .map {
                InvitationDto(
                    userId = it[ResidenceMembers.userId].toString(),
                    residenceId = residenceUuid.toString(),
                    role = it[ResidenceMembers.role].convert(),
                    status = it[ResidenceMembers.status].convert()
                )
            }
    }

    suspend fun deleteInvitation(
        requesterUuid: UUID,
        targetUserUuid: UUID,
        residenceUuid: UUID
    ): Map<String, String> = dbQuery {
        val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
        if (!isAdmin && requesterUuid != targetUserUuid) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
        }

        val deleted = ResidenceMembers.deleteWhere {
            (userId eq targetUserUuid) and (residenceId eq residenceUuid)
        }

        if (deleted == 0) throw HttpError(HttpStatusCode.NotFound, "Invitation non trouvée.")

        mapOf("status" to "Invitation supprimée")
    }
}
