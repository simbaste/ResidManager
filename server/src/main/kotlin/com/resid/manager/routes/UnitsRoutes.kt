package com.resid.manager.routes

import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.UnitService
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

fun Route.unitsRoutes() {
    route("/api/units") {
        authenticate("auth-jwt") {
            // POST /api/units?residenceId=... : Create a unit (Manager only)
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<ResidenceUnitCreateRequest>()
                    UnitService.createUnit(requesterId, residenceId, request)
                }
            }

            // PUT /api/units?residenceId=...&unitId=... : Update a unit (Manager only)
            put {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val unitId = call.getQueryUuid("unitId") ?: call.getPathUuid("unitId")
                    val request = call.receive<ResidenceUnitCreateRequest>()
                    UnitService.updateUnit(requesterId, residenceId, unitId, request)
                }
            }

            // GET /api/units?residenceId=... : List all units for a residence (Any authenticated user)
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val unitId = call.getQueryUuid("unitId")
                    UnitService.getUnits(residenceId, unitId)
                }
            }

            // GET /api/units/search?residenceId=...&q=...&memberId=...&status=... : Search units
            get("/search") {
                call.executeRequest(HttpStatusCode.OK) {
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val query = call.request.queryParameters["q"]
                    val memberId = call.getQueryUuid("memberId")
                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { UnitStatusDto.valueOf(it) }
                    }
                    UnitService.searchUnits(residenceId, query, memberId, status)
                }
            }

            // DELETE /api/units?residenceId=...&unitId=... : Delete a unit (Manager only)
            delete {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val unitId = call.getQueryUuid("unitId") ?: call.getPathUuid("unitId")
                    UnitService.deleteUnit(requesterId, residenceId, unitId)
                    mapOf("message" to "Le logement a été supprimé de la base de données avec succès !")
                }
            }
        }
    }
}
