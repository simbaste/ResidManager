package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.dto.InviteMemberRequest
import com.resid.manager.dto.InviteUpdateRequest
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.InvitationService
import com.resid.manager.service.executeRequest
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
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
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<InviteMemberRequest>()
                    InvitationService.inviteMember(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                }
            }

            // Update an invitation role (Admin only)
            put {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<InviteMemberRequest>()
                    InvitationService.updateInvitation(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                }
            }

            // Accept or Decline an invitation (Used by the related User)
            put("/process") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!
                    val request = call.receive<InviteUpdateRequest>()
                    InvitationService.processInvitation(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                }
            }

            // Get all invitations for the selected residence (Admin only or selected query user)
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val queryUserUuid = call.getQueryUuid("userId")
                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { InvitationStatus.valueOf(it) }
                            ?: throw HttpError(HttpStatusCode.BadRequest, "Statut d'invitation invalide.")
                    }

                    InvitationService.getInvitations(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        queryUserUuid = queryUserUuid,
                        status = status
                    )
                }
            }

            // Delete an invitation (only admin or the target user)
            delete {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val targetUserUuid = call.getQueryUuid("userId", required = true)!!
                    InvitationService.deleteInvitation(
                        requesterUuid = requesterUuid,
                        targetUserUuid = targetUserUuid,
                        residenceUuid = residenceUuid
                    )
                }
            }
        }
    }
}
