package com.resid.manager.routes

import com.resid.manager.data.ApplicationStatus
import com.resid.manager.data.HttpError
import com.resid.manager.data.Role
import com.resid.manager.dto.ApplicationRequest
import com.resid.manager.dto.ApplicationUpdateRequest
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.ApplicationService
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
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
import java.util.UUID

fun Route.applications() {
    route("/api/applications") {
        authenticate("auth-jwt") {
            // Get All applications (Filtered by residenceId, userId, status, or role)
            get {
                try {
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

                    val applications = ApplicationService.getApplications(
                        requesterUuid = requesterUuid,
                        residenceUuid = residenceUuid,
                        userUuid = userUuid,
                        status = status,
                        role = role
                    )
                    call.respond(applications)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur : ${e.message}"))
                }
            }

            // Apply to a residence (Ask to join a residence with a specific role)
            post {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!

                    val request = try {
                        call.receive<ApplicationRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    ApplicationService.applyToResidence(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid,
                        request = request
                    )
                    call.respond(HttpStatusCode.Created)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la création de la candidature : ${e.message}")
                    )
                }
            }

            // Update an application role (Only related user or an Admin)
            put {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!

                    val request = try {
                        call.receive<ApplicationRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    ApplicationService.updateApplication(
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
                        ErrorResponse("Erreur lors de la mise à jour : ${e.message}")
                    )
                }
            }

            // Process (Approve / Reject) an application (Admin only)
            put("/process") {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!

                    val request = try {
                        call.receive<ApplicationUpdateRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Requête invalide: ${e.message}")
                    }

                    ApplicationService.processApplication(
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
                        ErrorResponse("Erreur lors du traitement de la candidature : ${e.message}")
                    )
                }
            }

            // Delete an application (Only related user or an Admin)
            delete {
                try {
                    val requesterUuid = call.getRequesterUuid()
                    val residenceUuid = call.getQueryUuid("residenceId", required = true)!!
                    val userUuid = call.getQueryUuid("userId", required = true)!!

                    ApplicationService.deleteApplication(
                        requesterUuid = requesterUuid,
                        targetUserUuid = userUuid,
                        residenceUuid = residenceUuid
                    )
                    call.respond(HttpStatusCode.OK)
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
