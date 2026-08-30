package com.resid.manager.routes

import com.resid.manager.data.Equipment
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ExpenseRecordRequest
import com.resid.manager.service.DashboardService
import com.resid.manager.service.FinanceOperationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
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

            // -----------------------------------------------------------------
            // FINANCES & CASHFLOW ROUTES SECTION
            // -----------------------------------------------------------------
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

