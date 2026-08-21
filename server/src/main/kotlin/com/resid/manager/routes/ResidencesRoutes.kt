package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.MemberStatusUpdateRequest
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.service.ResidenceService
import com.resid.manager.service.getPathUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.residencesRoutes() {
    route("/api/residences") {
        // GET /api/residences/{id}/electricity/export-pdf : Generates the "Eco-Print" PDF
        get("/{id}/electricity/export-pdf") {
            try {
                val residenceId = call.getPathUuid("id")
                val statementIdsParam = call.request.queryParameters["ids"]

                val (pdfBytes, _) = ResidenceService.exportElectricityPdf(residenceId, statementIdsParam)
                call.respondBytes(pdfBytes, ContentType.Application.Pdf)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Erreur lors de l'export PDF : ${e.message}")
                )
            }
        }

        authenticate("auth-jwt") {
            // GET /api/residences : List residences for the directory
            get {
                try {
                    val userId = call.getRequesterUuid()
                    val directoryDto = ResidenceService.getDirectory(userId)
                    call.respond(HttpStatusCode.OK, directoryDto)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de l'annuaire : ${e.message}")
                    )
                }
            }

            // GET /api/residences/search?name=... : Search for a residence by name
            get("/search") {
                try {
                    val searchName = call.request.queryParameters["name"] ?: ""
                    val results = ResidenceService.searchResidences(searchName)
                    call.respond(HttpStatusCode.OK, results)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la recherche des résidences : ${e.message}")
                    )
                }
            }

            // GET /api/residences/{residenceId} : Get a specific residence summary
            get("/{residenceId}") {
                try {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("residenceId")
                    val residenceDto = ResidenceService.getResidenceById(userId, residenceId)
                    call.respond(HttpStatusCode.OK, residenceDto)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de la résidence : ${e.message}")
                    )
                }
            }

            // POST /api/residences : Create a residence
            post {
                try {
                    val userId = call.getRequesterUuid()
                    val request = try {
                        call.receive<ResidenceCreateRequest>()
                    } catch (e: ContentTransformationException) {
                        throw HttpError(HttpStatusCode.BadRequest, "Paramètre invalide: ${e.message}")
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Erreur lors de la lecture de la requête : ${e.message}")
                    }

                    val summary = ResidenceService.createResidence(userId, request)
                    call.respond(HttpStatusCode.Created, summary)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la création de la résidence : ${e.message}")
                    )
                }
            }

            // PUT /api/residences/{id} : Update a residence
            put("/{id}") {
                try {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")

                    val request = try {
                        call.receive<ResidenceCreateRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Paramètre invalide : ${e.message}")
                    }

                    val updatedSummary = ResidenceService.updateResidence(userId, residenceId, request)
                    call.respond(HttpStatusCode.OK, updatedSummary)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la modification de la résidence : ${e.message}")
                    )
                }
            }

            // DELETE /api/residences/{id} : Delete a residence
            delete("/{id}") {
                try {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")

                    ResidenceService.deleteResidence(userId, residenceId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "La résidence a été supprimée avec succès !"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression de la résidence : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/members/{user_id}/status : Admin accept/refuse member or update role
            post("/{id}/members/{user_id}/status") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")
                    val targetUserId = call.getPathUuid("user_id")
                    val request = call.receive<MemberStatusUpdateRequest>()

                    ResidenceService.adminUpdateMemberStatus(requesterId, residenceId, targetUserId, request)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "residenceId" to residenceId.toString(),
                            "targetUserId" to targetUserId.toString(),
                            "status" to "Statut mis à jour"
                        )
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du membre : ${e.message}")
                    )
                }
            }
        }
    }
}
