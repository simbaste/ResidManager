package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeasePaymentRequest
import com.resid.manager.dto.LeaseUpdateRequest
import com.resid.manager.service.LeaseService
import com.resid.manager.service.executeRequest
import com.resid.manager.service.getPathUuid
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
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
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterId = call.getRequesterUuid()
                    val unitId = call.getQueryUuid("unitId", required = true)!!
                    val request = call.receive<LeaseCreateRequest>()
                    LeaseService.createLease(requesterId, unitId, request)
                }
            }

            // PUT /api/leases/{leaseId} : Update a lease (Manager only)
            put("{leaseId}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getPathUuid("leaseId")
                    val request = call.receive<LeaseUpdateRequest>()
                    LeaseService.updateLease(requesterId, leaseId, request)
                }
            }

            // GET /api/leases?residenceId=... : List all leases for a residence (Any authenticated member)
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val residenceId = call.getQueryUuid("residenceId", required = true)!!
                    val leaseId = call.getQueryUuid("leaseId")
                    LeaseService.getLeasesByResidence(residenceId, leaseId)
                }
            }

            // PUT /api/leases/{leaseId}/payment : Record a payment (Manager only)
            put("{leaseId}/payment") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getPathUuid("leaseId")
                    val request = call.receive<LeasePaymentRequest>()
                    LeaseService.recordPayment(requesterId, leaseId, request)
                }
            }

            // PUT /api/leases/{leaseId}/status : Update lease status (Manager only)
            put("{leaseId}/status") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getPathUuid("leaseId")
                    val request = call.receive<LeaseUpdateRequest>()
                    val status = request.status
                        ?: throw HttpError(HttpStatusCode.BadRequest, "Statut de contrat de bail manquant.")
                    LeaseService.updateLeaseStatus(requesterId, leaseId, status)
                }
            }
        }
    }

    // Backward-compatible routes for existing clients
    authenticate("auth-jwt") {
        post("/api/logements/{id}/baux") {
            call.executeRequest(HttpStatusCode.Created) {
                val requesterId = call.getRequesterUuid()
                val unitId = call.getPathUuid("id")
                val request = call.receive<LeaseCreateRequest>()
                LeaseService.createLease(requesterId, unitId, request)
            }
        }

        get("/api/residences/{id}/baux") {
            call.executeRequest(HttpStatusCode.OK) {
                val residenceId = call.getPathUuid("id")
                LeaseService.getLeasesByResidence(residenceId, null)
            }
        }

        put("/api/baux/{id}/payment") {
            call.executeRequest(HttpStatusCode.OK) {
                val requesterId = call.getRequesterUuid()
                val leaseId = call.getPathUuid("id")
                val request = call.receive<LeasePaymentRequest>()
                LeaseService.recordPayment(requesterId, leaseId, request)
            }
        }

        put("/api/baux/{id}/status") {
            call.executeRequest(HttpStatusCode.OK) {
                val requesterId = call.getRequesterUuid()
                val leaseId = call.getPathUuid("id")
                val request = call.receive<LeaseUpdateRequest>()
                val status = request.status
                    ?: throw HttpError(HttpStatusCode.BadRequest, "Statut de contrat de bail manquant.")
                LeaseService.updateLeaseStatus(requesterId, leaseId, status)
            }
        }
    }
}
