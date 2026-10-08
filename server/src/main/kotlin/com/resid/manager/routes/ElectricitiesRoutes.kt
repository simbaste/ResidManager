package com.resid.manager.routes

import com.resid.manager.dto.ElectricityStatementCreateRequest
import com.resid.manager.dto.ElectricityStatementUpdateRequest
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ElectricityStatusUpdateRequest
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.ElectricityService
import com.resid.manager.service.executeRequest
import com.resid.manager.service.getPathUuid
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

fun Route.electricitiesRoutes() {
    route("/api/electricities") {
        authenticate("auth-jwt") {
            // POST /api/electricities : Create an electricity statement for a unit
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterId = call.getRequesterUuid()
                    val request = call.receive<ElectricityStatementCreateRequest>()
                    ElectricityService.createStatement(requesterId, request)
                }
            }

            // PUT /api/electricities/{id} : Update an electricity statement by ID
            put("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val statementId = call.getPathUuid("id")
                    val request = call.receive<ElectricityStatementUpdateRequest>()
                    ElectricityService.updateStatement(requesterId, statementId, request)
                }
            }

            // PUT /api/electricities/{id}/status : Update the status of an electricity statement
            put("/{id}/status") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val statementId = call.getPathUuid("id")
                    val request = call.receive<ElectricityStatusUpdateRequest>()
                    ElectricityService.updateStatementStatus(requesterId, statementId, request)
                }
            }

            // GET /api/electricities : List electricity statements with optional filters
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val unitId = call.getQueryUuid("unitId") ?: call.getQueryUuid("residenceUnitId")
                    val residenceId = call.getQueryUuid("residenceId")
                    val tenantId = call.getQueryUuid("tenantId")
                    val floor = call.request.queryParameters["floor"]
                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { ElectricityStatusDto.valueOf(it) }
                    }

                    ElectricityService.getStatements(
                        requesterId = requesterId,
                        residenceUnitId = unitId,
                        residenceId = residenceId,
                        tenantId = tenantId,
                        status = status,
                        floor = floor
                    )
                }
            }

            // GET /api/electricities/previous?unitId=... : Fetch previous locked meter index
            get("/previous") {
                call.executeRequest(HttpStatusCode.OK) {
                    val unitId = call.getQueryUuid("unitId") 
                        ?: call.getQueryUuid("residenceUnitId", required = true)!!
                    val previous = ElectricityService.getPreviousIndex(unitId)
                    mapOf("previousIndex" to previous)
                }
            }

            // DELETE /api/electricities/{id} : Delete electricity statement
            delete("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val statementId = call.getPathUuid("id")
                    ElectricityService.deleteStatement(statementId)
                    mapOf("message" to "Le relevé d'électricité a été supprimé avec succès !")
                }
            }
        }
    }

    // Backward-compatible routes for legacy client support
    authenticate("auth-jwt") {
        put("/api/electricity/statements/{id}/status") {
            call.executeRequest(HttpStatusCode.OK) {
                val requesterId = call.getRequesterUuid()
                val statementId = call.getPathUuid("id")
                val request = call.receive<ElectricityStatusUpdateRequest>()
                ElectricityService.updateStatementStatus(requesterId, statementId, request)
            }
        }

        get("/api/logements/{id}/electricity/previous") {
            call.executeRequest(HttpStatusCode.OK) {
                val residenceUnitId = call.getPathUuid("id")
                val previous = ElectricityService.getPreviousIndex(residenceUnitId)
                mapOf("previousIndex" to previous)
            }
        }

        post("/api/logements/{id}/electricity") {
            call.executeRequest(HttpStatusCode.Created) {
                val requesterId = call.getRequesterUuid()
                val residenceUnitId = call.getPathUuid("id")
                val request = call.receive<ElectricityStatementCreateRequest>()
                val effectiveReq = if (request.unitId.isBlank()) {
                    request.copy(unitId = residenceUnitId.toString())
                } else request

                ElectricityService.createStatement(requesterId, effectiveReq)
            }
        }
    }
}
