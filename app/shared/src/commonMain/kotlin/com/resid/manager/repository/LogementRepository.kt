package com.resid.manager.repository

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.LogementCreateRequest
import com.resid.manager.dto.LogementDto
import com.resid.manager.network.ApiClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface LogementRepository {
    suspend fun fetchLogements(token: String, residenceId: String): Result<List<LogementDto>>
    suspend fun createLogement(token: String, residenceId: String, request: LogementCreateRequest): Result<LogementDto>
}

class LogementRepositoryImpl(
    private val httpClient: HttpClient
) : LogementRepository {
    override suspend fun fetchLogements(token: String, residenceId: String): Result<List<LogementDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/logements") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<List<LogementDto>>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de chargement : ${e.message}"))
        }
    }

    override suspend fun createLogement(
        token: String,
        residenceId: String,
        request: LogementCreateRequest
    ): Result<LogementDto> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/logements") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body<LogementDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de création : ${e.message}"))
        }
    }
}
