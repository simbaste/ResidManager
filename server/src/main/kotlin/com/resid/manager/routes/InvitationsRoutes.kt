package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.data.isAdmin
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.InvitationDto
import com.resid.manager.dto.InviteMemberRequest
import com.resid.manager.dto.InviteUpdateRequest
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

fun Route.invitationsRoutes() {
    route("/api/invitations") {

        authenticate("auth-jwt") {

            // Invite a User (Only an Admin can do this)
            post("/residence/{residenceId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["residenceId"] ?: ""

                try {
                    // Validations
                    transaction {
                        // Check if residence exist
                        Residence.findById(UUID.fromString(residenceId)) ?: throw HttpError(
                            HttpStatusCode.NotFound,
                            "Résidence introuvable."
                        )

                        val isAdmin = ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }

                        if (!isAdmin) throw HttpError(
                            HttpStatusCode.Forbidden,
                            "Accès interdit: Seuls les propriétaires et administrateurs peuvent inviter des membres."
                        )
                    }

                    val request = try {
                        call.receive<InviteMemberRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    // Find invited user by email
                    val invitedUser = transaction {
                        User.find { Users.email eq request.email }.firstOrNull()
                    } ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable avec l'adresse email : ${request.email}")

                    // Check if an invitation already exists
                    transaction {
                        ResidenceMembers.select(ResidenceMembers.userId)
                            .where {
                                (ResidenceMembers.userId eq invitedUser.id.value) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId))
                            }.singleOrNull()
                    }?.let {
                        throw HttpError(HttpStatusCode.Conflict, "Une invitation existe déjà pour cet utilisateur.")
                    }

                    // Insert into residence_members with status 'INVITED' and role
                    transaction {
                        ResidenceMembers.insert {
                            it[ResidenceMembers.userId] = invitedUser.id.value
                            it[ResidenceMembers.residenceId] = UUID.fromString(residenceId)
                            it[ResidenceMembers.role] = request.role.convert()
                            it[ResidenceMembers.status] = InvitationStatus.INVITED
                            it[ResidenceMembers.createdAt] = LocalDateTime.now(Clock.systemUTC())
                            it[ResidenceMembers.updatedAt] = LocalDateTime.now(Clock.systemUTC())
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "status" to "Invitation envoyée"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de l'invitation : ${e.message}")
                    )
                }
            }

            // Update on invitation (Admin only)
            put("/residence/{residenceId}") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.getClaim("userId")?.asString() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val residenceId = call.parameters["residenceId"] ?: return@put call.respond(HttpStatusCode.BadRequest)

                try {
                    val request = try {
                        call.receive<InviteMemberRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }
                    val residenceUuid = try {
                        UUID.fromString(residenceId)
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "ID de résidence invalide: ${e.message}")
                    }

                    transaction {
                        val isAdmin = ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(currentUserId)) and
                                        (ResidenceMembers.residenceId eq residenceUuid) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }

                        if (!isAdmin) throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")

                        // Find invited user by email
                        val invitedUser = transaction {
                            User.find { Users.email eq request.email }.firstOrNull()
                        } ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable avec l'adresse email : ${request.email}")

                        val updated = ResidenceMembers.update({
                            (ResidenceMembers.userId eq invitedUser.id.value) and
                                    (ResidenceMembers.residenceId eq residenceUuid)
                        }) {
                            it[role] = request.role.convert()
                            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
                        }

                        if (updated == 0) throw HttpError(HttpStatusCode.NotFound, "Invitation non trouvée.")
                    }

                    call.respond(HttpStatusCode.OK, mapOf("status" to "Invitation mise à jour"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour : ${e.message}")
                    )
                }
            }

            // Accept or Decline an invitation (Used by the related User)
            put("/residence/{residenceId}/process") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.getClaim("userId")?.asString() ?: return@put call.respond(HttpStatusCode.Unauthorized)

                try {
                    val residenceUuid = call.parameters["residenceId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID de résidence invalide: ${e.message}")
                        }
                    } ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")

                    val userUuid = call.request.queryParameters["userId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID utilisateur invalide: ${e.message}")
                        }
                    } ?: throw HttpError(HttpStatusCode.BadRequest, "ID utilisateur manquant.")

                    if (userUuid != UUID.fromString(currentUserId)) {
                        throw HttpError(HttpStatusCode.Forbidden, "Accès interdit : vous ne pouvez modifier que votre propre invitation.")
                    }

                    // Check if residence exist
                    transaction {
                        Residence.findById(residenceUuid) ?: throw HttpError(
                            HttpStatusCode.NotFound,
                            "Résidence introuvable."
                        )
                    }

                    transaction {
                        // Check if invitation exist
                        ResidenceMembers.select(ResidenceMembers.userId)
                            .where {
                                (ResidenceMembers.userId eq userUuid) and
                                        (ResidenceMembers.residenceId eq residenceUuid)
                            }.singleOrNull() ?: throw HttpError(
                            HttpStatusCode.NotFound,
                            "Invitation introuvable."
                        )

                        // Check if the user is already invited
                        val status = ResidenceMembers.select(ResidenceMembers.status)
                            .where {
                                (ResidenceMembers.userId eq userUuid) and (ResidenceMembers.residenceId eq residenceUuid)
                            }
                            .singleOrNull()?.get(ResidenceMembers.status)
                        if (status == InvitationStatus.ACCEPTED) throw HttpError(
                            HttpStatusCode.NotAcceptable,
                            "Cette invitation n'est plus en attente."
                        )
                    }

                    val request = try {
                        call.receive<InviteUpdateRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    val updated = transaction {
                        ResidenceMembers.update({
                            (ResidenceMembers.userId eq userUuid) and
                                    (ResidenceMembers.residenceId eq residenceUuid)
                        }) {
                            it[status] = request.status.convert()
                            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
                        }
                    }
                    if (updated > 0) call.respond(HttpStatusCode.OK) else call.respond(
                        HttpStatusCode.NotFound
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour de l'invitation : ${e.message}")
                    )
                }
            }

            // Get all invitations for the selected residence (Admin only or selected query user)
            get("/residence/{residenceId}") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.getClaim("userId")?.asString() ?: return@get call.respond(HttpStatusCode.Unauthorized)

                try {
                    val residenceUuid = call.parameters["residenceId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID de résidence invalide: ${e.message}")
                        }
                    } ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")

                    val queryUserUuid = call.request.queryParameters["userId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID utilisateur invalide: ${e.message}")
                        }
                    }
                    val status = call.request.queryParameters["status"]?.let {
                        try {
                            InvitationStatus.valueOf(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "Statut d'invitation invalide.")
                        }
                    }

                    val isAdmin = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(currentUserId)) and
                                        (ResidenceMembers.residenceId eq residenceUuid) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }
                    }

                    if (!isAdmin && queryUserUuid != UUID.fromString(currentUserId)) {
                        throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
                    }

                    val invitations = transaction {
                        ResidenceMembers
                            .selectAll().where {
                                var baseQuery = ResidenceMembers.residenceId eq residenceUuid
                                if (queryUserUuid != null) {
                                    baseQuery = baseQuery and (ResidenceMembers.userId eq queryUserUuid)
                                }
                                if (status != null) {
                                    baseQuery = baseQuery and (ResidenceMembers.status eq status)
                                }
                                baseQuery
                            }
                            .map {
                                InvitationDto(
                                    userId = it[ResidenceMembers.userId].toString(),
                                    residenceId = residenceUuid.toString(),
                                    role = it[ResidenceMembers.role].convert(),
                                    status = it[ResidenceMembers.status].convert()
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, invitations)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur : ${e.message}"))
                }
            }

            // Delete an invitation (only admin or query user)
            delete("/residence/{residenceId}") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.getClaim("userId")?.asString() ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                try {
                    val residenceUuid = call.parameters["residenceId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID de résidence invalide: ${e.message}")
                        }
                    } ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")

                    val targetUserUuid = call.request.queryParameters["userId"]?.let {
                        try {
                            UUID.fromString(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "ID utilisateur invalide: ${e.message}")
                        }
                    } ?: throw HttpError(HttpStatusCode.BadRequest, "ID utilisateur manquant.")

                    transaction {
                        val isAdmin = ResidenceMembers.select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(currentUserId)) and
                                        (ResidenceMembers.residenceId eq residenceUuid) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }.any { it[ResidenceMembers.role].isAdmin() }

                        // Allow if Admin or if the user is deleting their own invitation
                        if (!isAdmin && UUID.fromString(currentUserId) != targetUserUuid) {
                            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit.")
                        }

                        val deleted = ResidenceMembers.deleteWhere {
                            (ResidenceMembers.userId eq targetUserUuid) and
                                    (ResidenceMembers.residenceId eq residenceUuid)
                        }

                        if (deleted == 0) throw HttpError(HttpStatusCode.NotFound, "Invitation non trouvée.")
                    }

                    call.respond(HttpStatusCode.OK, mapOf("status" to "Invitation supprimée"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la suppression : ${e.message}"))
                }
            }
        }
    }
}