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
    suspend fun deleteLogement(token: String, residenceId: String, logementId: String): Result<Unit>
    suspend fun updateLogement(token: String, residenceId: String, logementId: String, request: LogementCreateRequest): Result<LogementDto>
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

    override suspend fun deleteLogement(token: String, residenceId: String, logementId: String): Result<Unit> {
        return try {
            val response = httpClient.delete("${ApiClient.BASE_URL}/api/residences/$residenceId/logements/$logementId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de suppression : ${e.message}"))
        }
    }

    override suspend fun updateLogement(
        token: String,
        residenceId: String,
        logementId: String,
        request: LogementCreateRequest
    ): Result<LogementDto> {
        return try {
            val response = httpClient.put("${ApiClient.BASE_URL}/api/residences/$residenceId/logements/$logementId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<LogementDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de modification : ${e.message}"))
        }
    }
}
