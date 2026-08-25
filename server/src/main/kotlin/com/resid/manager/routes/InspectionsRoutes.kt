package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.InspectionReportCreateRequest
import com.resid.manager.dto.InspectionReportUpdateRequest
import com.resid.manager.service.InspectionService
import com.resid.manager.service.getPathUuid
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.inspectionsRoutes() {
    route("/api/inspections") {
        authenticate("auth-jwt") {
            // POST /api/inspections : Create an inspection report
            post {
                try {
                    val requesterId = call.getRequesterUuid()
                    val request = call.receive<InspectionReportCreateRequest>()
                    val createdReport = InspectionService.createReport(requesterId, request)
                    call.respond(HttpStatusCode.Created, createdReport)
                } catch (e: ContentTransformationException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(e.message ?: "Impossible de convertir le corps de la requête.")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la création de l'état des lieux : ${e.message}")
                    )
                }
            }

            // GET /api/inspections?leaseId=... : Get inspection reports for a lease
            get {
                try {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getQueryUuid("leaseId", required = true)!!
                    val reports = InspectionService.getReportsByLease(requesterId, leaseId)
                    call.respond(HttpStatusCode.OK, reports)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des états des lieux : ${e.message}")
                    )
                }
            }

            // GET /api/inspections/{id} : Get a single inspection report by ID
            get("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val reportId = call.getPathUuid("id")
                    val report = InspectionService.getReportById(requesterId, reportId)
                    call.respond(HttpStatusCode.OK, report)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de l'état des lieux : ${e.message}")
                    )
                }
            }

            // PUT /api/inspections/{id} : Update an inspection report
            put("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val reportId = call.getPathUuid("id")
                    val request = call.receive<InspectionReportUpdateRequest>()
                    val updatedReport = InspectionService.updateReport(requesterId, reportId, request)
                    call.respond(HttpStatusCode.OK, updatedReport)
                } catch (e: ContentTransformationException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(e.message ?: "Impossible de convertir le corps de la requête.")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour de l'état des lieux : ${e.message}")
                    )
                }
            }

            // DELETE /api/inspections/{id} : Delete an inspection report
            delete("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val reportId = call.getPathUuid("id")
                    InspectionService.deleteReport(requesterId, reportId)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "L'état des lieux a été supprimé avec succès !")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression de l'état des lieux : ${e.message}")
                    )
                }
            }
        }
    }
}
