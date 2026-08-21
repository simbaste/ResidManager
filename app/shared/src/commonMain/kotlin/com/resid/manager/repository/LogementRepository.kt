package com.resid.manager.repository

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.network.ApiClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface ResidenceUnitRepository {
    suspend fun fetchResidenceUnits(token: String, residenceId: String): Result<List<ResidenceUnitDto>>
    suspend fun createResidenceUnit(token: String, residenceId: String, request: ResidenceUnitCreateRequest): Result<ResidenceUnitDto>
    suspend fun deleteResidenceUnit(token: String, residenceId: String, residenceUnitId: String): Result<Unit>
    suspend fun updateResidenceUnit(token: String, residenceId: String, residenceUnitId: String, request: ResidenceUnitCreateRequest): Result<ResidenceUnitDto>
}

class ResidenceUnitRepositoryImpl(
    private val httpClient: HttpClient
) : ResidenceUnitRepository {
    override suspend fun fetchResidenceUnits(token: String, residenceId: String): Result<List<ResidenceUnitDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/logements") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<List<ResidenceUnitDto>>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de chargement : ${e.message}"))
        }
    }

    override suspend fun createResidenceUnit(
        token: String,
        residenceId: String,
        request: ResidenceUnitCreateRequest
    ): Result<ResidenceUnitDto> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/logements") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceUnitDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de création : ${e.message}"))
        }
    }

    override suspend fun deleteResidenceUnit(token: String, residenceId: String, residenceUnitId: String): Result<Unit> {
        return try {
            val response = httpClient.delete("${ApiClient.BASE_URL}/api/residences/$residenceId/logements/$residenceUnitId") {
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

    override suspend fun updateResidenceUnit(
        token: String,
        residenceId: String,
        residenceUnitId: String,
        request: ResidenceUnitCreateRequest
    ): Result<ResidenceUnitDto> {
        return try {
            val response = httpClient.put("${ApiClient.BASE_URL}/api/residences/$residenceId/logements/$residenceUnitId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceUnitDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de modification : ${e.message}"))
        }
    }
}
