package com.resid.manager.routes

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
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceApplicationDto
import com.resid.manager.helpers.tryOptional
import com.resid.manager.helpers.tryOptionalSuspend
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

fun Route.applications() {
    route("/api/applications") {
        authenticate("auth-jwt") {
            // Get All the applications (Only an Admin or the related user can use this)
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                try {
                    // Validations
                    val residenceId = call.request.queryParameters["residenceId"]
                        ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")
                    val residenceUuid = try {
                        UUID.fromString(residenceId)
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "ID de résidence invalide: ${e.message}")
                    }

                    val applicantUserUuid = call.request.queryParameters["userId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID utilisateur invalide: ${e.message}")
                        }
                    }

                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { ApplicationStatus.valueOf(it) } ?: throw HttpError(HttpStatusCode.BadRequest, "Statut de la candidature invalide.")
                    }
                    val role = call.request.queryParameters["role"]?.let {
                        tryOptional { Role.valueOf(it) } ?: throw HttpError(HttpStatusCode.BadRequest, "Rôle de la candidature invalide.")
                    }

                    val isAdmin = transaction {
                        ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }
                    }

                    if (!isAdmin && userId != applicantUserUuid?.toString()) {
                        throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
                    }

                    val applications = transaction {
                        val residence = Residence.findById(UUID.fromString(residenceId))
                            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

                        (Users innerJoin ResidenceApplications)
                            .select(ResidenceApplications.userId, ResidenceApplications.status,
                                ResidenceApplications.role, ResidenceApplications.createdAt,
                                ResidenceApplications.updatedAt, Users.firstName, Users.lastName,
                                Users.email, Users.phone)
                            .where {
                                (ResidenceApplications.residenceId eq UUID.fromString(residenceId)) and
                                        (if (applicantUserUuid != null) ResidenceApplications.userId eq applicantUserUuid else Op.TRUE) and
                                        (if (status != null) ResidenceApplications.status eq status else Op.TRUE) and
                                        (if (role != null) ResidenceApplications.role eq role else Op.TRUE)
                            }.map {
                                ResidenceApplicationDto(
                                    residenceId = residenceId,
                                    residenceName = residence.name,
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
                    call.respond(applications)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur : ${e.message}"))
                }
            }

            // Apply to a residence (Ask to join a residence with a specific role: Owner, Admin, or Member)
            post {
                val principal = call.principal<JWTPrincipal>()
                principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = tryOptionalSuspend { call.receive<ApplicationRequest>() }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Requête invalide."))

                try {
                    transaction {
                        // Check if residence exist
                        Residence.findById(UUID.fromString(request.residenceId))
                            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

                        // Check if an application already exist
                        val existingApplication = ResidenceApplications.select(ResidenceApplications.userId)
                            .where {
                                (ResidenceApplications.userId eq UUID.fromString(request.userId)) and
                                        (ResidenceApplications.residenceId eq UUID.fromString(request.residenceId))
                            }.singleOrNull()

                        if (existingApplication != null) {
                            throw HttpError(HttpStatusCode.Conflict, "Une candidature existe déjà pour cet utilisateur.")
                        }

                        ResidenceApplications.insert {
                            it[ResidenceApplications.userId] = UUID.fromString(request.userId)
                            it[ResidenceApplications.residenceId] = UUID.fromString(request.residenceId)
                            it[ResidenceApplications.role] = request.role.convert()
                            it[ResidenceApplications.createdAt] = LocalDateTime.now(Clock.systemUTC())
                            it[ResidenceApplications.updatedAt] = LocalDateTime.now(Clock.systemUTC())
                            it[ResidenceApplications.status] = ApplicationStatus.PENDING
                        }
                    }
                    call.respond(HttpStatusCode.Created)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la création de la candidature : ${e.message}"))
                }
            }

            // Update an application (Only the related user or an Admin can do this)
            put {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = tryOptionalSuspend { call.receive<ApplicationRequest>() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Requête invalide."))

                try {
                    transaction {
                        // Check if the user is the owner or an admin
                        val isResidenceAdmin = ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(request.residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }

                        if (userId != request.userId && !isResidenceAdmin) {
                            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
                        }

                        val updatedRows = ResidenceApplications.update({
                            (ResidenceApplications.userId eq UUID.fromString(request.userId)) and
                                    (ResidenceApplications.residenceId eq UUID.fromString(request.residenceId))
                        }) {
                            it[role] = request.role.convert()
                            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
                        }
                        if (updatedRows == 0) throw HttpError(HttpStatusCode.NotFound, "Candidature introuvable.")
                    }
                    call.respond(HttpStatusCode.OK)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la mise à jour : ${e.message}"))
                }
            }

            // Accept or Decline an application
            put("/process") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = tryOptionalSuspend { call.receive<ApplicationUpdateRequest>() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Requête invalide."))

                try {
                    transaction {
                        // Check if the user is the owner or an admin
                        val isAdmin = ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(request.residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }

                        if (!isAdmin) {
                            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
                        }

                        val updatedRows = ResidenceApplications.update({
                            (ResidenceApplications.userId eq UUID.fromString(request.userId)) and
                                    (ResidenceApplications.residenceId eq UUID.fromString(request.residenceId))
                        }) {
                            it[role] = request.role.convert()
                            it[status] = request.status.convert()
                            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
                        }
                        if (updatedRows == 0) throw HttpError(HttpStatusCode.NotFound, "Candidature introuvable.")

                        // Add user in ResidenceMembers Table
                        if (request.status == ApplicationStatusDto.APPROVED) {
                            ResidenceMembers.insert {
                                it[ResidenceMembers.userId] = UUID.fromString(request.userId)
                                it[ResidenceMembers.residenceId] =
                                    UUID.fromString(request.residenceId)
                                it[ResidenceMembers.role] = request.role.convert()
                                it[ResidenceMembers.createdAt] = LocalDateTime.now(Clock.systemUTC())
                                it[ResidenceMembers.updatedAt] = LocalDateTime.now(Clock.systemUTC())
                                it[ResidenceMembers.status] = InvitationStatus.ACCEPTED
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la mise à jour : ${e.message}"))
                }
            }

            // Delete a specific application (Only related user or an Admin can do this)
            delete {
                val principal = call.principal<JWTPrincipal>()
                val requesterId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val request = tryOptionalSuspend { call.receive<ApplicationRequest>() }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Requête invalide."))

                try {
                    transaction {
                        val isResidenceAdmin = ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(requesterId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(request.residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }

                        if (requesterId != request.userId && !isResidenceAdmin) {
                            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
                        }

                        val deletedRows = ResidenceApplications.deleteWhere {
                            (userId eq UUID.fromString(request.userId)) and (residenceId eq UUID.fromString(request.residenceId))
                        }
                        if (deletedRows == 0) throw HttpError(HttpStatusCode.NotFound, "Candidature introuvable.")
                    }
                    call.respond(HttpStatusCode.OK)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la suppression : ${e.message}"))
                }
            }
        }
    }
}