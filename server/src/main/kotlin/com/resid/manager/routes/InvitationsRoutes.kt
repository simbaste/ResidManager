package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.InviteMemberRequest
import com.resid.manager.dto.InviteUpdateRequest
import com.resid.manager.service.InvitationService
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.invitationsRoutes() {
    route("/api/invitations") {
        authenticate("auth-jwt") {
            // Invite a User (Only an Admin can do this)
            post {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!

                    val request = try {
                        call.receive<InviteMemberRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    val response = InvitationService.inviteMember(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de l'invitation : ${e.message}")
                    )
                }
            }

            // Update an invitation role (Admin only)
            put {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!

                    val request = try {
                        call.receive<InviteMemberRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    val response = InvitationService.updateInvitation(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                    call.respond(HttpStatusCode.OK, response)
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
            put("/process") {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!

                    val request = try {
                        call.receive<InviteUpdateRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    InvitationService.processInvitation(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                    call.respond(HttpStatusCode.OK)
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
            get {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val queryUserUuid = call.getQueryUuid("userId")
                    val status = call.request.queryParameters["status"]?.let {
                        try {
                            InvitationStatus.valueOf(it)
                        } catch (e: Exception) {
                            throw HttpError(HttpStatusCode.BadRequest, "Statut d'invitation invalide.")
                        }
                    }

                    val invitations = InvitationService.getInvitations(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        queryUserUuid = queryUserUuid,
                        status = status
                    )
                    call.respond(HttpStatusCode.OK, invitations)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur : ${e.message}"))
                }
            }

            // Delete an invitation (only admin or the target user)
            delete {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val targetUserUuid = call.getQueryUuid("userId", required = true)!!

                    val response = InvitationService.deleteInvitation(
                        requesterUuid = requesterUuid,
                        targetUserUuid = targetUserUuid,
                        residenceUuid = residenceUuid
                    )
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression : ${e.message}")
                    )
                }
            }
        }
    }
}
