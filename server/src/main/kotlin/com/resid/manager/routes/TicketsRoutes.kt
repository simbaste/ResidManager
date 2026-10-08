package com.resid.manager.routes

import com.resid.manager.dto.TicketCategoryRequest
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.TicketUpdateRequest
import com.resid.manager.dto.TicketUrgencyDto
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.TicketService
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

fun Route.ticketsRoutes() {
    // -------------------------------------------------------------
    // Ticket Categories Routing
    // -------------------------------------------------------------
    route("/api/ticket-categories") {
        authenticate("auth-jwt") {
            // GET /api/ticket-categories?residenceId=... : List categories
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val residenceId = call.getQueryUuid("residenceId")
                    TicketService.getCategories(residenceId)
                }
            }

            // POST /api/ticket-categories?residenceId=... : Create category
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<TicketCategoryRequest>()
                    TicketService.createCategory(requesterId, residenceId, request)
                }
            }

            // PUT /api/ticket-categories/{id} : Update category
            put("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val categoryId = call.getPathUuid("id")
                    val request = call.receive<TicketCategoryRequest>()
                    TicketService.updateCategory(requesterId, categoryId, request)
                }
            }

            // DELETE /api/ticket-categories/{id} : Delete custom category
            delete("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val categoryId = call.getPathUuid("id")
                    TicketService.deleteCategory(requesterId, categoryId)
                    mapOf("message" to "Catégorie de ticket supprimée avec succès !")
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Tickets Routing
    // -------------------------------------------------------------
    route("/api/tickets") {
        authenticate("auth-jwt") {
            // POST /api/tickets : Create a maintenance ticket
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val creatorId = call.getRequesterUuid()
                    val request = call.receive<TicketCreateRequest>()
                    TicketService.createTicket(creatorId, request)
                }
            }

            // GET /api/tickets : List tickets with filters (residenceId, unitId, status, urgency, creatorId)
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId")
                    val unitId = call.getQueryUuid("unitId") ?: call.getQueryUuid("residenceUnitId")
                    val creatorId = call.getQueryUuid("creatorId")
                    val status = call.request.queryParameters["status"]?.let {
                        tryOptional { TicketStatusDto.valueOf(it) }
                    }
                    val urgency = call.request.queryParameters["urgency"]?.let {
                        tryOptional { TicketUrgencyDto.valueOf(it) }
                    }

                    TicketService.getTickets(
                        requesterId = requesterId,
                        residenceId = residenceId,
                        unitId = unitId,
                        status = status,
                        urgency = urgency,
                        creatorId = creatorId
                    )
                }
            }

            // GET /api/tickets/{id} : Get ticket by ID
            get("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val ticketId = call.getPathUuid("id")
                    TicketService.getTicketById(requesterId, ticketId)
                }
            }

            // PUT /api/tickets/{id}/status : Update status (OPEN -> IN_PROGRESS -> CLOSED)
            put("/{id}/status") {
                call.executeRequest(HttpStatusCode.OK) {
                    val updaterId = call.getRequesterUuid()
                    val ticketId = call.getPathUuid("id")
                    val request = call.receive<TicketUpdateRequest>()
                    TicketService.updateTicketStatus(updaterId, ticketId, request)
                }
            }

            // DELETE /api/tickets/{id} : Delete ticket
            delete("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val ticketId = call.getPathUuid("id")
                    TicketService.deleteTicket(requesterId, ticketId)
                    mapOf("message" to "Ticket supprimé avec succès !")
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Backward-compatible routes for legacy clients
    // -------------------------------------------------------------
    authenticate("auth-jwt") {
        get("/api/residences/{id}/ticket-categories") {
            call.executeRequest(HttpStatusCode.OK) {
                val residenceId = call.getPathUuid("id")
                TicketService.getCategories(residenceId)
            }
        }

        post("/api/residences/{id}/ticket-categories") {
            call.executeRequest(HttpStatusCode.Created) {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                val request = call.receive<TicketCategoryRequest>()
                TicketService.createCategory(requesterId, residenceId, request)
            }
        }

        get("/api/residences/{id}/tickets") {
            call.executeRequest(HttpStatusCode.OK) {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                TicketService.getTickets(
                    requesterId = requesterId,
                    residenceId = residenceId,
                    unitId = null,
                    status = null,
                    urgency = null,
                    creatorId = null
                )
            }
        }

        post("/api/logements/{id}/tickets") {
            call.executeRequest(HttpStatusCode.Created) {
                val creatorId = call.getRequesterUuid()
                val residenceUnitId = call.getPathUuid("id")
                val request = call.receive<TicketCreateRequest>()
                val effectiveReq = if (request.unitId.isBlank()) {
                    request.copy(unitId = residenceUnitId.toString())
                } else request

                TicketService.createTicket(creatorId, effectiveReq)
            }
        }
    }
}
