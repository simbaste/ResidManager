package com.resid.manager.features.tickets.data

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketDto
import com.resid.manager.dto.TicketUpdateRequest
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface TicketRepository {
    suspend fun fetchTickets(token: String, residenceId: String): Result<List<TicketDto>>
    suspend fun fetchCategories(token: String, residenceId: String): Result<List<TicketCategoryDto>>
    suspend fun createTicket(token: String, unitId: String, request: TicketCreateRequest): Result<TicketDto>
    suspend fun updateTicketStatus(token: String, ticketId: String, request: TicketUpdateRequest): Result<TicketDto>
}

class TicketRepositoryImpl : TicketRepository {
    override suspend fun fetchTickets(token: String, residenceId: String): Result<List<TicketDto>> {
        return try {
            val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/tickets") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchCategories(token: String, residenceId: String): Result<List<TicketCategoryDto>> {
        return try {
            val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/ticket-categories") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTicket(token: String, unitId: String, request: TicketCreateRequest): Result<TicketDto> {
        return try {
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/logements/$unitId/tickets") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTicketStatus(token: String, ticketId: String, request: TicketUpdateRequest): Result<TicketDto> {
        return try {
            val response = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/tickets/$ticketId/status") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
