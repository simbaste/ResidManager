package com.resid.manager.routes

import com.resid.manager.data.ApplicationStatus
import com.resid.manager.data.HttpError
import com.resid.manager.data.Role
import com.resid.manager.dto.ApplicationRequest
import com.resid.manager.dto.ApplicationUpdateRequest
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.ApplicationService
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

fun Route.applications() {
    route("/api/applications") {
        authenticate("auth-jwt") {
            // Get All applications (Filtered by residenceId, userId, status, or role)
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId")
                    val userUuid = call.getQueryUuid("userId")

                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { ApplicationStatus.valueOf(it) }
                            ?: throw HttpError(HttpStatusCode.BadRequest, "Statut de la candidature invalide.")
                    }
                    val role = call.request.queryParameters["role"]?.let {
                        tryOptional { Role.valueOf(it) }
                            ?: throw HttpError(HttpStatusCode.BadRequest, "Rôle de la candidature invalide.")
                    }

                    ApplicationService.getApplications(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        userUuid = userUuid,
                        status = status,
                        role = role
                    )
                }
            }

            // Apply to a residence (Ask to join a residence with a specific role)
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!
                    val request = call.receive<ApplicationRequest>()

                    ApplicationService.applyToResidence(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                }
            }

            // Update an application role (Only related user or an Admin)
            put {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!
                    val request = call.receive<ApplicationRequest>()

                    ApplicationService.updateApplication(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                }
            }

            // Process (Approve / Reject) an application (Admin only)
            put("/process") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!
                    val request = call.receive<ApplicationUpdateRequest>()

                    ApplicationService.processApplication(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                }
            }

            // Delete an application (Only related user or an Admin)
            delete {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!

                    ApplicationService.deleteApplication(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid
                    )
                }
            }
        }
    }
}
