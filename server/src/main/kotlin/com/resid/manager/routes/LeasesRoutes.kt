package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeasePaymentRequest
import com.resid.manager.dto.LeaseUpdateRequest
import com.resid.manager.service.LeaseService
import com.resid.manager.service.getPathUuid
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.leasesRoutes() {
    route("/api/leases") {
        authenticate("auth-jwt") {
            // POST /api/leases?unitId=... : Create a lease for a unit (Manager only)
            post {
                try {
                    val requesterId = call.getRequesterUuid()
                    val unitId = call.getQueryUuid("unitId", required = true)!!
                    val request = call.receive<LeaseCreateRequest>()

                    val createdLease = LeaseService.createLease(requesterId, unitId, request)
                    call.respond(HttpStatusCode.Created, createdLease)
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
                        ErrorResponse("Erreur lors de la création du contrat de bail : ${e.message}")
                    )
                }
            }

            // PUT /api/leases/{leaseId}... : Update a lease (Manager only)
            put("{leaseId}") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getPathUuid("leaseId")
                    val request = call.receive<LeaseUpdateRequest>()

                    val updatedLease = LeaseService.updateLease(requesterId, leaseId, request)
                    call.respond(HttpStatusCode.OK, updatedLease)
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
                        ErrorResponse("Erreur lors de la mise à jour du contrat de bail : ${e.message}")
                    )
                }
            }

            // GET /api/leases?residenceId=... : List all leases for a residence (Any authenticated member)
            get {
                try {
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val leaseId = call.getQueryUuid("leaseId")
                    val leases = LeaseService.getLeasesByResidence(residenceId, leaseId)
                    call.respond(HttpStatusCode.OK, leases)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des contrats de bail : ${e.message}")
                    )
                }
            }

            // PUT /api/leases/{leaseId}/payment... : Record a payment (Manager only)
            put("{leaseId}/payment") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getPathUuid("leaseId")
                    val request = call.receive<LeasePaymentRequest>()

                    val updatedLease = LeaseService.recordPayment(requesterId, leaseId, request)
                    call.respond(HttpStatusCode.OK, updatedLease)
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
                        ErrorResponse("Erreur lors de l'enregistrement du paiement : ${e.message}")
                    )
                }
            }

            // PUT /api/leases/{leaseId}/status... : Update lease status (Manager only)
            put("{leaseId}/status") {
                try {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getPathUuid("leaseId")
                    val request = call.receive<LeaseUpdateRequest>()

                    val status = request.status
                        ?: throw HttpError(HttpStatusCode.BadRequest, "Statut de contrat de bail manquant.")

                    val updatedLease = LeaseService.updateLeaseStatus(requesterId, leaseId, status)
                    call.respond(HttpStatusCode.OK, updatedLease)
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
                        ErrorResponse("Erreur lors de la mise à jour du statut du bail : ${e.message}")
                    )
                }
            }
        }
    }

    // Backward-compatible routes for existing clients
    authenticate("auth-jwt") {
        // POST /api/logements/{id}/baux : Create lease
        post("/api/logements/{id}/baux") {
            try {
                val requesterId = call.getRequesterUuid()
                val unitId = call.getPathUuid("id")
                val request = call.receive<LeaseCreateRequest>()

                val createdLease = LeaseService.createLease(requesterId, unitId, request)
                call.respond(HttpStatusCode.Created, createdLease)
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
                    ErrorResponse("Erreur lors de la création du contrat de bail : ${e.message}")
                )
            }
        }

        // GET /api/residences/{id}/baux : List leases for a residence
        get("/api/residences/{id}/baux") {
            try {
                val residenceId = call.getPathUuid("id")
                val leases = LeaseService.getLeasesByResidence(residenceId, null)
                call.respond(HttpStatusCode.OK, leases)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Erreur lors de la récupération des contrats de bail : ${e.message}")
                )
            }
        }

        // PUT /api/baux/{id}/payment : Record payment
        put("/api/baux/{id}/payment") {
            try {
                val requesterId = call.getRequesterUuid()
                val leaseId = call.getPathUuid("id")
                val request = call.receive<LeasePaymentRequest>()

                val updatedLease = LeaseService.recordPayment(requesterId, leaseId, request)
                call.respond(HttpStatusCode.OK, updatedLease)
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
                    ErrorResponse("Erreur lors de l'enregistrement du paiement : ${e.message}")
                )
            }
        }

        // PUT /api/baux/{id}/status : Update status
        put("/api/baux/{id}/status") {
            try {
                val requesterId = call.getRequesterUuid()
                val leaseId = call.getPathUuid("id")
                val request = call.receive<LeaseUpdateRequest>()

                val status = request.status
                    ?: throw HttpError(HttpStatusCode.BadRequest, "Statut de contrat de bail manquant.")

                val updatedLease = LeaseService.updateLeaseStatus(requesterId, leaseId, status)
                call.respond(HttpStatusCode.OK, updatedLease)
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
                    ErrorResponse("Erreur lors de la mise à jour du statut du bail : ${e.message}")
                )
            }
        }
    }
}
