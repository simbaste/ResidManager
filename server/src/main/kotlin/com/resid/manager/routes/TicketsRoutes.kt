package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.TicketCategoryRequest
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.TicketUpdateRequest
import com.resid.manager.dto.TicketUrgencyDto
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.TicketService
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

fun Route.ticketsRoutes() {
    // -------------------------------------------------------------
    // Ticket Categories Routing
    // -------------------------------------------------------------
    route("/api/ticket-categories") {
        authenticate("auth-jwt") {
            // GET /api/ticket-categories?residenceId=... : List categories
            get {
                try {
                    val residenceId = call.getQueryUuid("residenceId")
                    val categories = TicketService.getCategories(residenceId)
                    call.respond(HttpStatusCode.OK, categories)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur de chargement des catégories : ${e.message}")
                    )
                }
            }

            // POST /api/ticket-categories?residenceId=... : Create category
            post {
                try {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<TicketCategoryRequest>()
                    val created = TicketService.createCategory(requesterId, residenceId, request)
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
                        ErrorResponse("Erreur lors de la création de la catégorie : ${e.message}")
                    )
                }
            }

            // PUT /api/ticket-categories/{id} : Update category
            put("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val categoryId = call.getPathUuid("id")
                    val request = call.receive<TicketCategoryRequest>()
                    val updated = TicketService.updateCategory(requesterId, categoryId, request)
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
                        ErrorResponse("Erreur lors de la modification de la catégorie : ${e.message}")
                    )
                }
            }

            // DELETE /api/ticket-categories/{id} : Delete custom category
            delete("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val categoryId = call.getPathUuid("id")
                    TicketService.deleteCategory(requesterId, categoryId)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Catégorie de ticket supprimée avec succès !")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression de la catégorie : ${e.message}")
                    )
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
                try {
                    val creatorId = call.getRequesterUuid()
                    val request = call.receive<TicketCreateRequest>()
                    val created = TicketService.createTicket(creatorId, request)
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
                        ErrorResponse("Erreur lors de la création du ticket : ${e.message}")
                    )
                }
            }

            // GET /api/tickets : List tickets with filters (residenceId, unitId, status, urgency, creatorId)
            get {
                try {
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

                    val tickets = TicketService.getTickets(
                        requesterId = requesterId,
                        residenceId = residenceId,
                        unitId = unitId,
                        status = status,
                        urgency = urgency,
                        creatorId = creatorId
                    )
                    call.respond(HttpStatusCode.OK, tickets)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des tickets : ${e.message}")
                    )
                }
            }

            // GET /api/tickets/{id} : Get ticket by ID
            get("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val ticketId = call.getPathUuid("id")
                    val ticket = TicketService.getTicketById(requesterId, ticketId)
                    call.respond(HttpStatusCode.OK, ticket)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération du ticket : ${e.message}")
                    )
                }
            }

            // PUT /api/tickets/{id}/status : Update status (OPEN -> IN_PROGRESS -> CLOSED)
            put("/{id}/status") {
                try {
                    val updaterId = call.getRequesterUuid()
                    val ticketId = call.getPathUuid("id")
                    val request = call.receive<TicketUpdateRequest>()
                    val updated = TicketService.updateTicketStatus(updaterId, ticketId, request)
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
                        ErrorResponse("Erreur lors de la transition d'état du ticket : ${e.message}")
                    )
                }
            }

            // DELETE /api/tickets/{id} : Delete ticket
            delete("/{id}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val ticketId = call.getPathUuid("id")
                    TicketService.deleteTicket(requesterId, ticketId)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Ticket supprimé avec succès !")
                    )
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression du ticket : ${e.message}")
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Backward-compatible routes for legacy clients
    // -------------------------------------------------------------
    authenticate("auth-jwt") {
        get("/api/residences/{id}/ticket-categories") {
            try {
                val residenceId = call.getPathUuid("id")
                val categories = TicketService.getCategories(residenceId)
                call.respond(HttpStatusCode.OK, categories)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Erreur de chargement des catégories.")
                )
            }
        }

        post("/api/residences/{id}/ticket-categories") {
            try {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                val request = call.receive<TicketCategoryRequest>()
                val created = TicketService.createCategory(requesterId, residenceId, request)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(e.message ?: "Erreur de création de catégorie.")
                )
            }
        }

        get("/api/residences/{id}/tickets") {
            try {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                val tickets = TicketService.getTickets(
                    requesterId = requesterId,
                    residenceId = residenceId,
                    unitId = null,
                    status = null,
                    urgency = null,
                    creatorId = null
                )
                call.respond(HttpStatusCode.OK, tickets)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Erreur de chargement des tickets.")
                )
            }
        }

        post("/api/logements/{id}/tickets") {
            try {
                val creatorId = call.getRequesterUuid()
                val residenceUnitId = call.getPathUuid("id")
                val request = call.receive<TicketCreateRequest>()
                val effectiveReq = if (request.unitId.isBlank()) {
                    request.copy(unitId = residenceUnitId.toString())
                } else request

                val created = TicketService.createTicket(creatorId, effectiveReq)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(e.message ?: "Erreur lors de la création du ticket.")
                )
            }
        }
    }
}
