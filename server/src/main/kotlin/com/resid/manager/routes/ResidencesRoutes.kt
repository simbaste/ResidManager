package com.resid.manager.routes

import com.resid.manager.dto.MemberStatusUpdateRequest
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.service.ResidenceService
import com.resid.manager.service.executeRequest
import com.resid.manager.service.getPathUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
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
            } catch (e: Exception) {
                call.executeRequest { throw e }
            }
        }

        authenticate("auth-jwt") {
            // GET /api/residences : List residences for the directory
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    ResidenceService.getDirectory(userId)
                }
            }

            // GET /api/residences/search?name=... : Search for a residence by name
            get("/search") {
                call.executeRequest(HttpStatusCode.OK) {
                    val searchName = call.request.queryParameters["name"] ?: ""
                    ResidenceService.searchResidences(searchName)
                }
            }

            // GET /api/residences/{residenceId} : Get a specific residence summary
            get("/{residenceId}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("residenceId")
                    ResidenceService.getResidenceById(userId, residenceId)
                }
            }

            // POST /api/residences : Create a residence
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val userId = call.getRequesterUuid()
                    val request = call.receive<ResidenceCreateRequest>()
                    ResidenceService.createResidence(userId, request)
                }
            }

            // PUT /api/residences/{id} : Update a residence
            put("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")
                    val request = call.receive<ResidenceCreateRequest>()
                    ResidenceService.updateResidence(userId, residenceId, request)
                }
            }

            // DELETE /api/residences/{id} : Delete a residence
            delete("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")
                    ResidenceService.deleteResidence(userId, residenceId)
                    mapOf("message" to "La résidence a été supprimée avec succès !")
                }
            }

            // GET /api/residences/{id}/members : Get all residence members
            get("/{id}/members") {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")
                    ResidenceService.getResidenceMembers(userId, residenceId)
                }
            }

            // POST /api/residences/{id}/members/{user_id}/status : Admin accept/refuse member or update role
            post("/{id}/members/{user_id}/status") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getPathUuid("id")
                    val targetUserId = call.getPathUuid("user_id")
                    val request = call.receive<MemberStatusUpdateRequest>()
                    ResidenceService.adminUpdateMemberStatus(requesterId, residenceId, targetUserId, request)
                    mapOf(
                        "residenceId" to residenceId.toString(),
                        "targetUserId" to targetUserId.toString(),
                        "status" to "Statut mis à jour"
                    )
                }
            }
        }
    }
}
