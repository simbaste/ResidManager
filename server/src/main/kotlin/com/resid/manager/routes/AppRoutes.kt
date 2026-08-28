package com.resid.manager.routes

import com.resid.manager.data.Equipment
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.Ticket
import com.resid.manager.data.TicketCategories
import com.resid.manager.data.TicketCategoryEntity
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ExpenseRecordRequest
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketDto
import com.resid.manager.dto.TicketUpdateRequest
import com.resid.manager.service.DashboardService
import com.resid.manager.service.FinanceOperationService
import com.resid.manager.service.TicketService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Application.configureAppRoutes() {
    routing {
        // -----------------------------------------------------------------
        // SECTION 2: SECURED ROUTES (JWT AUTHENTICATED)
        // -----------------------------------------------------------------
        // GET /api/equipements : List predefined equipments (Publicly accessible helper)
        get("/api/equipements") {
            try {
                val list = transaction {
                    Equipment.all().map {
                        EquipmentDto(id = it.id.value.toString(), key = it.key, label = it.label)
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Erreur lors de la récupération des équipements : ${e.message}")
                )
            }
        }

        authenticate("auth-jwt") {
            // GET /api/residences/{residence_id}/members Get All the residence members
            get("/api/residences/{id}/members") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    // Check if member exists in residence_members with ACCEPTED status
                    val isMember = transaction {
                        !ResidenceMembers
                            .select(ResidenceMembers.userId)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .empty()
                    }

                    if (!isMember) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : vous ne faites pas partie de cette résidence."))
                        return@get
                    }

                    val membersList = transaction {
                        // Join Users with ResidenceMembers
                        (Users innerJoin ResidenceMembers)
                            .select(
                                Users.id,
                                Users.firstName,
                                Users.lastName,
                                Users.email,
                                Users.phone,
                                ResidenceMembers.role,
                                ResidenceMembers.status
                            )
                            .where { ResidenceMembers.residenceId eq UUID.fromString(residenceId) }
                            .map { row ->
                                ResidenceMemberSummaryDto(
                                    userId = row[Users.id].value.toString(),
                                    firstName = row[Users.firstName],
                                    lastName = row[Users.lastName],
                                    email = row[Users.email],
                                    phone = row[Users.phone],
                                    roleDto = row[ResidenceMembers.role].convert(),
                                    status = row[ResidenceMembers.status].convert()
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, membersList)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de la liste des membres : ${e.message}")
                    )
                }
            }

            // -----------------------------------------------------------------
            // FINANCES & CASHFLOW ROUTES SECTION
            // -----------------------------------------------------------------
            // GET /api/residences/{id}/ticket-categories : List all ticket categories (global and custom to residence)
            get("/api/residences/{id}/ticket-categories") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val list = transaction {
                        TicketCategoryEntity.all().filter { 
                            it.residence?.id?.value == null || it.residence?.id?.value == UUID.fromString(residenceId) 
                        }.map {
                            TicketCategoryDto(
                                id = it.id.value.toString(),
                                key = it.key,
                                label = it.label,
                                residenceId = it.residence?.id?.value?.toString()
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, list)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement des catégories."))
                }
            }

            // POST /api/residences/{id}/ticket-categories : Create custom ticket category
            post("/api/residences/{id}/ticket-categories") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCategoryDto>()
                    val created = transaction {
                        val dbResidence = Residence.findById(UUID.fromString(residenceId))
                            ?: throw Exception("Résidence introuvable.")

                        // Check if key already exists
                        val exists = TicketCategoryEntity.find { TicketCategories.key eq request.key.uppercase() }.count() > 0
                        if (exists) throw Exception("Cette clé de catégorie existe déjà.")

                        val entity = TicketCategoryEntity.new {
                            this.residence = dbResidence
                            this.key = request.key.uppercase()
                            this.label = request.label
                            this.createdAt = LocalDateTime.now(Clock.systemUTC())
                            this.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        }
                        entity.flush()

                        TicketCategoryDto(
                            id = entity.id.value.toString(),
                            key = entity.key,
                            label = entity.label,
                            residenceId = entity.residence?.id?.value?.toString()
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de création de catégorie."))
                }
            }

            // PUT /api/ticket-categories/{id} : Update ticket category
            put("/api/ticket-categories/{id}") {
                val categoryId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCategoryDto>()
                    val updated = transaction {
                        val entity = TicketCategoryEntity.findById(UUID.fromString(categoryId))
                            ?: throw Exception("Catégorie de ticket introuvable.")

                        entity.label = request.label
                        entity.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        entity.flush()

                        TicketCategoryDto(
                            id = entity.id.value.toString(),
                            key = entity.key,
                            label = entity.label,
                            residenceId = entity.residence?.id?.value?.toString()
                        )
                    }
                    call.respond(HttpStatusCode.OK, updated)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de modification de catégorie."))
                }
            }

            // DELETE /api/ticket-categories/{id} : Delete custom ticket category
            delete("/api/ticket-categories/{id}") {
                val categoryId = call.parameters["id"] ?: ""
                try {
                    val success = transaction {
                        val entity = TicketCategoryEntity.findById(UUID.fromString(categoryId))
                            ?: throw Exception("Catégorie de ticket introuvable.")

                        if (entity.residence == null) {
                            throw Exception("Action interdite : Les catégories globales par défaut ne peuvent pas être supprimées.")
                        }

                        entity.delete()
                        true
                    }
                    call.respond(HttpStatusCode.OK, mapOf("success" to success, "message" to "Catégorie supprimée avec succès."))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de suppression de catégorie."))
                }
            }

            // GET /api/residences/{id}/tickets : List all tickets for this residence
            get("/api/residences/{id}/tickets") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val ticketsList = transaction {
                        Ticket.all().filter { it.residenceUnit.residence.id.value == UUID.fromString(residenceId) }.map {
                            TicketDto(
                                id = it.id.value.toString(),
                                residenceUnitId = it.residenceUnit.id.value.toString(),
                                creatorId = it.creator.id.value.toString(),
                                category = TicketCategoryDto(
                                    id = it.category.id.value.toString(),
                                    key = it.category.key,
                                    label = it.category.label,
                                    residenceId = it.category.residence?.id?.value?.toString()
                                ),
                                title = it.title,
                                description = it.description,
                                urgency = it.urgency.convert(),
                                status = it.status.convert(),
                                interventionCost = it.interventionCost,
                                createdAt = it.createdAt.toString(),
                                updatedAt = it.updatedAt.toString()
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, ticketsList)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement des tickets."))
                }
            }

            // POST /api/logements/{id}/tickets : Open a maintenance ticket
            post("/api/logements/{id}/tickets") {
                val residenceUnitId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCreateRequest>()
                    val principal = call.principal<JWTPrincipal>()
                    val creatorIdStr = principal?.payload?.getClaim("userId")?.asString() 
                        ?: throw Exception("Utilisateur non authentifié.")

                    val created = transaction {
                        TicketService.createTicket(
                            residenceUnitId = UUID.fromString(residenceUnitId),
                            creatorId = UUID.fromString(creatorIdStr),
                            categoryId = UUID.fromString(request.categoryId),
                            title = request.title,
                            description = request.description,
                            urgency = request.urgency
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur lors de la création du ticket."))
                }
            }

            // PUT /api/tickets/{id}/status : Update status (OPEN -> IN_PROGRESS -> CLOSED)
            put("/api/tickets/{id}/status") {
                val ticketId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketUpdateRequest>()
                    val principal = call.principal<JWTPrincipal>()
                    val updaterIdStr = principal?.payload?.getClaim("userId")?.asString()
                        ?: throw Exception("Utilisateur non authentifié.")

                    val updated = transaction {
                        TicketService.updateTicketStatus(
                            ticketId = UUID.fromString(ticketId),
                            newStatus = request.status ?: throw Exception("Statut de transition manquant."),
                            cost = request.interventionCost,
                            comment = request.comment,
                            updaterUserId = UUID.fromString(updaterIdStr)
                        )
                    }
                    call.respond(HttpStatusCode.OK, updated)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur lors de la transition d'état du ticket."))
                }
            }

            // GET /api/residences/{id}/transactions : List operations ledger
            get("/api/residences/{id}/transactions") {
                val residenceId = call.parameters["id"] ?: ""
                val typeParam = call.request.queryParameters["type"]
                val categoryParam = call.request.queryParameters["category"]
                val startDateParam = call.request.queryParameters["start_date"]
                val endDateParam = call.request.queryParameters["end_date"]
                val queryParam = call.request.queryParameters["q"]

                try {
                    val list = transaction {
                        FinanceOperationService.getTransactions(
                            residenceId = UUID.fromString(residenceId),
                            typeParam = typeParam,
                            categoryParam = categoryParam,
                            startDateParam = startDateParam,
                            endDateParam = endDateParam,
                            queryParam = queryParam
                        )
                    }
                    call.respond(HttpStatusCode.OK, list)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement du grand livre."))
                }
            }

            // POST /api/residences/{id}/transactions : Record manual operational expense
            post("/api/residences/{id}/transactions") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<ExpenseRecordRequest>()
                    val created = transaction {
                        FinanceOperationService.recordExpense(
                            residenceId = UUID.fromString(residenceId),
                            categoryName = request.category,
                            amount = request.amount,
                            description = request.description,
                            date = LocalDate.parse(request.transactionDate)
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de saisie de la dépense."))
                }
            }

            // GET /api/residences/{id}/dashboard : Returns Cashflow, occupancy rate, delinquency rate
            get("/api/residences/{id}/dashboard") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                // Context Guard & Middleware: verify user role inside residence_members
                val userRole = transaction {
                    ResidenceMembers
                        .select(ResidenceMembers.role)
                        .where {
                            (ResidenceMembers.userId eq UUID.fromString(userId)) and
                            (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                            (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                        }
                        .singleOrNull()?.get(ResidenceMembers.role)
                }

                if (userRole == null) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Accès interdit: Vous ne faites pas partie de cette résidence.")
                    )
                    return@get
                }

                val filterParam = call.request.queryParameters["filter"] ?: "MONTH"
                val startDate = call.request.queryParameters["start_date"]
                val endDate = call.request.queryParameters["end_date"]

                try {
                    val data = DashboardService.getDashboardData(
                        residenceId = UUID.fromString(residenceId),
                        filterType = filterParam,
                        customStart = startDate,
                        customEnd = endDate
                    )
                    call.respond(HttpStatusCode.OK, data)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur lors du calcul analytique du tableau de bord."))
                }
            }
        }
    }
}
