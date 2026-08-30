package com.resid.manager.service

import com.resid.manager.data.ApplicationStatus
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceApplications
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.Residences
import com.resid.manager.data.Role
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.data.isAdmin
import com.resid.manager.dto.ApplicationRequest
import com.resid.manager.dto.ApplicationStatusDto
import com.resid.manager.dto.ApplicationUpdateRequest
import com.resid.manager.dto.ResidenceApplicationDto
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

object ApplicationService {

    suspend fun isResidenceAdmin(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isAdmin() }
    }

    suspend fun getApplications(
        requesterUuid: UUID,
        residenceUuid: UUID?,
        userUuid: UUID?,
        status: ApplicationStatus?,
        role: Role?
    ): List<ResidenceApplicationDto> = dbQuery {
        if (residenceUuid != null) {
            val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
            if (!isAdmin && requesterUuid != userUuid) {
                throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
            }
        }

        var conditions: Op<Boolean> = Op.TRUE
        if (residenceUuid != null) conditions = conditions and (ResidenceApplications.residenceId eq residenceUuid)
        if (userUuid != null) conditions = conditions and (ResidenceApplications.userId eq userUuid)
        if (status != null) conditions = conditions and (ResidenceApplications.status eq status)
        if (role != null) conditions = conditions and (ResidenceApplications.role eq role)

        (Users innerJoin ResidenceApplications innerJoin Residences)
            .select(
                ResidenceApplications.userId, ResidenceApplications.status,
                Residences.id, Residences.name,
                ResidenceApplications.role, ResidenceApplications.createdAt,
                ResidenceApplications.updatedAt, Users.firstName, Users.lastName,
                Users.email, Users.phone
            )
            .where { conditions }
            .map {
                ResidenceApplicationDto(
                    residenceId = it[Residences.id].toString(),
                    residenceName = it[Residences.name],
                    firstName = it[Users.firstName],
                    lastName = it[Users.lastName],
                    email = it[Users.email],
                    phone = it[Users.phone],
                    userId = it[ResidenceApplications.userId].toString(),
                    roleDto = it[ResidenceApplications.role].convert(),
                    status = it[ResidenceApplications.status].convert(),
                    createdAt = it[ResidenceApplications.createdAt].toString(),
                    updatedAt = it[ResidenceApplications.updatedAt].toString()
                )
            }
    }

    suspend fun applyToResidence(
        requesterUuid: UUID,
        targetUserUuid: UUID,
        residenceUuid: UUID,
        request: ApplicationRequest
    ) = dbQuery {
        val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
        if (requesterUuid != targetUserUuid && !isAdmin) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
        }

        // Check if residence exists
        Residence.findById(residenceUuid)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        // Check if an application already exists
        val existingApplication = ResidenceApplications.select(ResidenceApplications.userId)
            .where {
                (ResidenceApplications.userId eq targetUserUuid) and
                (ResidenceApplications.residenceId eq residenceUuid)
            }.singleOrNull()

        if (existingApplication != null) {
            throw HttpError(HttpStatusCode.Conflict, "Une candidature existe déjà pour cet utilisateur.")
        }

        // Check if the user is already a residence member
        val isMember = ResidenceMembers.select(ResidenceMembers.userId)
            .where {
                (ResidenceMembers.residenceId eq residenceUuid) and
                (ResidenceMembers.userId eq targetUserUuid) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.count() > 0

        if (isMember) {
            throw HttpError(HttpStatusCode.Conflict, "L'utilisateur est déjà membre de la résidence.")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        ResidenceApplications.insert {
            it[userId] = targetUserUuid
            it[residenceId] = residenceUuid
            it[role] = request.role.convert()
            it[createdAt] = now
            it[updatedAt] = now
            it[status] = ApplicationStatus.PENDING
        }
    }

    suspend fun updateApplication(
        requesterUuid: UUID,
        targetUserUuid: UUID,
        residenceUuid: UUID,
        request: ApplicationRequest
    ) = dbQuery {
        val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
        if (requesterUuid != targetUserUuid && !isAdmin) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        val updatedRows = ResidenceApplications.update({
            (ResidenceApplications.userId eq targetUserUuid) and
            (ResidenceApplications.residenceId eq residenceUuid)
        }) {
            it[role] = request.role.convert()
            it[updatedAt] = now
        }
        if (updatedRows == 0) throw HttpError(HttpStatusCode.NotFound, "Candidature introuvable.")
    }

    suspend fun processApplication(
        requesterUuid: UUID,
        targetUserUuid: UUID,
        residenceUuid: UUID,
        request: ApplicationUpdateRequest
    ) = dbQuery {
        val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
        if (!isAdmin) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit : seuls les administrateurs peuvent traiter une candidature.")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        val updatedRows = ResidenceApplications.update({
            (ResidenceApplications.userId eq targetUserUuid) and
            (ResidenceApplications.residenceId eq residenceUuid)
        }) {
            it[role] = request.role.convert()
            it[status] = request.status.convert()
            it[updatedAt] = now
        }
        if (updatedRows == 0) throw HttpError(HttpStatusCode.NotFound, "Candidature introuvable.")

        // Add or update user in ResidenceMembers Table on approval
        if (request.status == ApplicationStatusDto.APPROVED) {
            val existing = ResidenceMembers.select(ResidenceMembers.userId)
                .where {
                    (ResidenceMembers.userId eq targetUserUuid) and
                    (ResidenceMembers.residenceId eq residenceUuid)
                }
                .singleOrNull()

            if (existing == null) {
                ResidenceMembers.insert {
                    it[userId] = targetUserUuid
                    it[residenceId] = residenceUuid
                    it[role] = request.role.convert()
                    it[createdAt] = now
                    it[updatedAt] = now
                    it[status] = InvitationStatus.ACCEPTED
                }
            } else {
                ResidenceMembers.update({
                    (ResidenceMembers.userId eq targetUserUuid) and
                    (ResidenceMembers.residenceId eq residenceUuid)
                }) {
                    it[role] = request.role.convert()
                    it[status] = InvitationStatus.ACCEPTED
                    it[updatedAt] = now
                }
            }
        }
    }

    suspend fun deleteApplication(
        requesterUuid: UUID,
        targetUserUuid: UUID,
        residenceUuid: UUID
    ) = dbQuery {
        val isAdmin = isResidenceAdmin(requesterUuid, residenceUuid)
        if (requesterUuid != targetUserUuid && !isAdmin) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
        }

        val deletedRows = ResidenceApplications.deleteWhere {
            (userId eq targetUserUuid) and (residenceId eq residenceUuid)
        }
        if (deletedRows == 0) throw HttpError(HttpStatusCode.NotFound, "Candidature introuvable.")
    }
}
