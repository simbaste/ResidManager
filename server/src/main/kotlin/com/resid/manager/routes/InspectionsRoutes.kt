package com.resid.manager.routes

import com.resid.manager.dto.InspectionReportCreateRequest
import com.resid.manager.dto.InspectionReportUpdateRequest
import com.resid.manager.service.InspectionService
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

fun Route.inspectionsRoutes() {
    route("/api/inspections") {
        authenticate("auth-jwt") {
            // POST /api/inspections : Create an inspection report
            post {
                call.executeRequest(HttpStatusCode.Created) {
                    val requesterId = call.getRequesterUuid()
                    val request = call.receive<InspectionReportCreateRequest>()
                    InspectionService.createReport(requesterId, request)
                }
            }

            // GET /api/inspections?leaseId=... : Get inspection reports for a lease
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val leaseId = call.getQueryUuid("leaseId", required = true)!!
                    InspectionService.getReportsByLease(requesterId, leaseId)
                }
            }

            // GET /api/inspections/{id} : Get a single inspection report by ID
            get("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val reportId = call.getPathUuid("id")
                    InspectionService.getReportById(requesterId, reportId)
                }
            }

            // PUT /api/inspections/{id} : Update an inspection report
            put("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val reportId = call.getPathUuid("id")
                    val request = call.receive<InspectionReportUpdateRequest>()
                    InspectionService.updateReport(requesterId, reportId, request)
                }
            }

            // DELETE /api/inspections/{id} : Delete an inspection report
            delete("/{id}") {
                call.executeRequest(HttpStatusCode.OK) {
                    val requesterId = call.getRequesterUuid()
                    val reportId = call.getPathUuid("id")
                    InspectionService.deleteReport(requesterId, reportId)
                    mapOf("message" to "L'état des lieux a été supprimé avec succès !")
                }
            }
        }
    }
}
