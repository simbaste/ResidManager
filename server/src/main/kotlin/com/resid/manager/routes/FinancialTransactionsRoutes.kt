package com.resid.manager.routes

import com.resid.manager.dto.ExpenseRecordRequest
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.TransactionTypeDto
import com.resid.manager.dto.TransactionUpdateRequest
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.FinancialTransactionService
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

fun Route.financialTransactionsRoutes() {
    route("/api/financial-transactions") {
        authenticate("auth-jwt") {
            // POST /api/financial-transactions?residenceId=... : Record an expense
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val request = call.receive<ExpenseRecordRequest>()
                    FinancialTransactionService.recordExpense(requesterId, residenceId, request)
                }
            }

            // GET /api/financial-transactions?residenceId=... : List transactions
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val type = call.request.queryParameters["type"]?.let {
                        tryOptional { TransactionTypeDto.valueOf(it.uppercase()) }
                    }
                    val category = call.request.queryParameters["category"]?.let {
                        tryOptional { TransactionCategoryDto.valueOf(it.uppercase()) }
                    }
                    val startDate = call.request.queryParameters["start_date"]
                    val endDate = call.request.queryParameters["end_date"]
                    val queryText = call.request.queryParameters["q"]

                    FinancialTransactionService.getTransactions(
                        requesterId = requesterId,
                        residenceId = residenceId,
                        type = type,
                        category = category,
                        startDate = startDate,
                        endDate = endDate,
                        queryText = queryText
                    )
                }
            }

            // GET /api/financial-transactions/{id} : Get transaction by ID
            get("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val transactionId = call.getPathUuid("id")
                    FinancialTransactionService.getTransactionById(requesterId, transactionId)
                }
            }

            // PUT /api/financial-transactions/{id} : Update transaction (Manager only)
            put("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val transactionId = call.getPathUuid("id")
                    val request = call.receive<TransactionUpdateRequest>()
                    FinancialTransactionService.updateTransaction(requesterId, transactionId, request)
                }
            }

            // DELETE /api/financial-transactions/{id} : Delete transaction (Manager only)
            delete("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val transactionId = call.getPathUuid("id")
                    FinancialTransactionService.deleteTransaction(requesterId, transactionId)
                    mapOf("message" to "Transaction financière supprimée avec succès !")
                }
            }
        }
    }

    // Backward-compatible routes for existing client code
    authenticate("auth-jwt") {
        get("/api/residences/{id}/transactions") {
            call.executeRequest(HttpStatusCode.OK) {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                val type = call.request.queryParameters["type"]?.let {
                    tryOptional { TransactionTypeDto.valueOf(it.uppercase()) }
                }
                val category = call.request.queryParameters["category"]?.let {
                    tryOptional { TransactionCategoryDto.valueOf(it.uppercase()) }
                }
                val startDate = call.request.queryParameters["start_date"]
                val endDate = call.request.queryParameters["end_date"]
                val queryText = call.request.queryParameters["q"]

                FinancialTransactionService.getTransactions(
                    requesterId = requesterId,
                    residenceId = residenceId,
                    type = type,
                    category = category,
                    startDate = startDate,
                    endDate = endDate,
                    queryText = queryText
                )
            }
        }

        post("/api/residences/{id}/transactions") {
            call.executeRequest(HttpStatusCode.Created) {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                val request = call.receive<ExpenseRecordRequest>()
                FinancialTransactionService.recordExpense(requesterId, residenceId, request)
            }
        }

        post("/api/residences/{id}/depenses") {
            call.executeRequest(HttpStatusCode.Created) {
                val requesterId = call.getRequesterUuid()
                val residenceId = call.getPathUuid("id")
                val request = call.receive<ExpenseRecordRequest>()
                FinancialTransactionService.recordExpense(requesterId, residenceId, request)
            }
        }
    }
}
