package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ElectricityStatementCreateRequest
import com.resid.manager.dto.ElectricityStatementUpdateRequest
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ElectricityStatusUpdateRequest
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.ElectricityService
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

fun Route.electricitiesRoutes() {
    route("/api/electricities") {
        authenticate("auth-jwt") {
            // POST /api/electricities : Create an electricity statement for a unit
            post {
                try {
                    val requesterId = call.getRequesterUuid()
                    val request = call.receive<ElectricityStatementCreateRequest>()
                    val created = ElectricityService.createStatement(requesterId, request)
                    call.respond(HttpStatusCode.Created, created)
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
                        ErrorResponse("Erreur lors de la création du relevé d'électricité : ${e.message}")
                    )
                    e.printStackTrace()
                }
            }

            // PUT /api/electricities/{id} : Update an electricity statement by ID
            put("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val statementId = call.getPathUuid("id")
                    val request = call.receive<ElectricityStatementUpdateRequest>()
                    val updated = ElectricityService.updateStatement(requesterId, statementId, request)
                    call.respond(HttpStatusCode.OK, updated)
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
                        ErrorResponse("Erreur lors de la mise à jour du relevé d'électricité : ${e.message}")
                    )
                    e.printStackTrace()
                }
            }

            // PUT /api/electricities/{id}/status : Update the status of an electricity statement
            put("/{id}/status") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val statementId = call.getPathUuid("id")
                    val request = call.receive<ElectricityStatusUpdateRequest>()
                    val updated = ElectricityService.updateStatementStatus(requesterId, statementId, request)
                    call.respond(HttpStatusCode.OK, updated)
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
                        ErrorResponse("Erreur lors de la mise à jour du statut du relevé : ${e.message}")
                    )
                    e.printStackTrace()
                }
            }

            // GET /api/electricities : List electricity statements with optional filters (unit, residence, tenant, status, floor)
            get {
                try {
                    val requesterId = call.getRequesterUuid()
                    val unitId = call.getQueryUuid("unitId") ?: call.getQueryUuid("residenceUnitId")
                    val residenceId = call.getQueryUuid("residenceId")
                    val tenantId = call.getQueryUuid("tenantId")
                    val floor = call.request.queryParameters["floor"]
                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { ElectricityStatusDto.valueOf(it) }
                    }

                    val statements = ElectricityService.getStatements(
                        requesterId = requesterId,
                        residenceUnitId = unitId,
                        residenceId = residenceId,
                        tenantId = tenantId,
                        status = status,
                        floor = floor
                    )
                    call.respond(HttpStatusCode.OK, statements)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des relevés d'électricité : ${e.message}")
                    )
                    e.printStackTrace()
                }
            }

            // GET /api/electricities/previous?unitId=... : Fetch previous locked meter index
            get("/previous") {
                try {
                    val unitId = call.getQueryUuid("unitId") 
                        ?: call.getQueryUuid("residenceUnitId", required = true)!!
                    val previous = ElectricityService.getPreviousIndex(unitId)
                    call.respond(HttpStatusCode.OK, mapOf("previousIndex" to previous))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Erreur de chargement de l'index précédent.")
                    )
                    e.printStackTrace()
                }
            }

            // DELETE /api/electricities/id... : Delete electricity statement
            delete("/{id}") {
                try {
                    val statementId = call.getPathUuid("id")
                    ElectricityService.deleteStatement(statementId)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Le relevé d'électricité a été supprimé avec succès !")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Erreur lors de la suppression de la quitance d'électricité")
                    )
                    e.printStackTrace()
                }
            }
        }
    }

    // Backward-compatible routes for legacy client support
    authenticate("auth-jwt") {
        put("/api/electricity/statements/{id}/status") {
            try {
                val requesterId = call.getRequesterUuid()
                val statementId = call.getPathUuid("id")
                val request = call.receive<ElectricityStatusUpdateRequest>()
                val updated = ElectricityService.updateStatementStatus(requesterId, statementId, request)
                call.respond(HttpStatusCode.OK, updated)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de mise à jour."))
            }
        }

        get("/api/logements/{id}/electricity/previous") {
            try {
                val residenceUnitId = call.getPathUuid("id")
                val previous = ElectricityService.getPreviousIndex(residenceUnitId)
                call.respond(HttpStatusCode.OK, mapOf("previousIndex" to previous))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Erreur de chargement de l'index précédent.")
                )
            }
        }

        post("/api/logements/{id}/electricity") {
            try {
                val requesterId = call.getRequesterUuid()
                val residenceUnitId = call.getPathUuid("id")
                val request = call.receive<ElectricityStatementCreateRequest>()
                val effectiveReq = if (request.unitId.isBlank()) {
                    request.copy(unitId = residenceUnitId.toString())
                } else request

                val created = ElectricityService.createStatement(requesterId, effectiveReq)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Erreur lors de la création du relevé.")
                )
            }
        }
    }
}
