package com.resid.manager.repository

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.network.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

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
            val response = httpClient.get("${ApiClient.BASE_URL}/api/units") {
                parameter("residenceId", residenceId)
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
            val response = httpClient.post("${ApiClient.BASE_URL}/api/units") {
                parameter("residenceId", residenceId)
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
            val response = httpClient.delete("${ApiClient.BASE_URL}/api/units") {
                parameter("residenceId", residenceId)
                parameter("unitId", residenceUnitId)
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
            val response = httpClient.put("${ApiClient.BASE_URL}/api/units") {
                parameter("residenceId", residenceId)
                parameter("unitId", residenceUnitId)
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
