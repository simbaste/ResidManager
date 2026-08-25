package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.UnitService
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

fun Route.unitsRoutes() {
    route("/api/units") {
        authenticate("auth-jwt") {
            // POST /api/units?residenceId=... : Create a unit (Manager only)
            post {
                try {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<ResidenceUnitCreateRequest>()

                    val createdUnit = UnitService.createUnit(requesterId, residenceId, request)
                    call.respond(HttpStatusCode.Created, createdUnit)
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
                        ErrorResponse("Erreur lors de la création du logement : ${e.message}")
                    )
                }
            }

            // PUT /api/units?residenceId=...&unitId=... : Update a unit (Manager only)
            put {
                try {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val unitId = call.getQueryUuid("unitId", required = true)!!
                    val request = call.receive<ResidenceUnitCreateRequest>()

                    val updatedUnit = UnitService.updateUnit(requesterId, residenceId, unitId, request)
                    call.respond(HttpStatusCode.OK, updatedUnit)
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
                        ErrorResponse("Erreur lors de la modification du logement : ${e.message}")
                    )
                }
            }

            // GET /api/units?residenceId=... : List all units for a residence (Any authenticated user)
            get {
                try {
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val unitId = call.getQueryUuid("unitId")
                    val units = UnitService.getUnits(residenceId, unitId)
                    call.respond(HttpStatusCode.OK, units)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des logements : ${e.message}")
                    )
                }
            }

            // GET /api/units/search?residenceId=...&q=...&memberId=...&status=... : Search units
            get("/search") {
                try {
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val query = call.request.queryParameters["q"]
                    val memberId = call.getQueryUuid("memberId")
                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { UnitStatusDto.valueOf(it) }
                    }

                    val units = UnitService.searchUnits(residenceId, query, memberId, status)
                    call.respond(HttpStatusCode.OK, units)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la recherche des logements : ${e.message}")
                    )
                }
            }

            // DELETE /api/units?residenceId=...&unitId=... : Delete a unit (Manager only)
            delete {
                try {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val unitId = call.getQueryUuid("unitId", required = true)!!

                    UnitService.deleteUnit(requesterId, residenceId, unitId)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Le logement a été supprimé de la base de données avec succès !")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression du logement : ${e.message}")
                    )
                }
            }
        }
    }
}
