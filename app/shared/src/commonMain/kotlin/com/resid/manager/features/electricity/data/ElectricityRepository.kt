package com.resid.manager.features.electricity.data

import com.resid.manager.dto.ElectricityStatementCreateRequest
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ElectricityStatusUpdateRequest
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
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

interface ElectricityRepository {
    suspend fun fetchStatements(token: String, residenceId: String): Result<List<ElectricityStatementDto>>
    suspend fun fetchPreviousIndex(token: String, unitId: String): Result<Double>
    suspend fun createStatement(token: String, residenceId: String, request: ElectricityStatementCreateRequest): Result<ElectricityStatementDto>
    suspend fun markStatementPaid(token: String, statementId: String): Result<ElectricityStatementDto>
}

class ElectricityRepositoryImpl : ElectricityRepository {
    override suspend fun fetchStatements(token: String, residenceId: String): Result<List<ElectricityStatementDto>> {
        return try {
            val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/electricities") {
                parameter("residenceId", residenceId)
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

    override suspend fun fetchPreviousIndex(token: String, unitId: String): Result<Double> {
        return try {
            val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/electricities/previous") {
                parameter("unitId", unitId)
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.body<Map<String, Double>>()
                Result.success(body["previousIndex"] ?: 0.0)
            } else {
                Result.failure(Exception("Impossible de récupérer le dernier index"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createStatement(
        token: String,
        residenceId: String,
        request: ElectricityStatementCreateRequest
    ): Result<ElectricityStatementDto> {
        return try {
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/electricities") {
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

    override suspend fun markStatementPaid(token: String, statementId: String): Result<ElectricityStatementDto> {
        return try {
            val response = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/electricities/$statementId/status") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(ElectricityStatusUpdateRequest(status = ElectricityStatusDto.PAID))
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
