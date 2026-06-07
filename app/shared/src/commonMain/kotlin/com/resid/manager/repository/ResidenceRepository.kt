package com.resid.manager.repository

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceDirectoryDTO
import com.resid.manager.dto.ResidenceSummaryItem
import com.resid.manager.network.ApiClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface ResidenceRepository {
    suspend fun fetchResidences(token: String): Result<ResidenceDirectoryDTO>
    suspend fun createResidence(token: String, request: ResidenceCreateRequest): Result<ResidenceSummaryItem>
    suspend fun searchResidences(token: String, name: String): Result<List<ResidenceSummaryItem>>
}

class ResidenceRepositoryImpl(
    private val httpClient: HttpClient
) : ResidenceRepository {
    override suspend fun fetchResidences(token: String): Result<ResidenceDirectoryDTO> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceDirectoryDTO>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de récupération : ${e.message}"))
        }
    }

    override suspend fun createResidence(token: String, request: ResidenceCreateRequest): Result<ResidenceSummaryItem> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/residences") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceSummaryItem>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de création : ${e.message}"))
        }
    }

    override suspend fun searchResidences(token: String, name: String): Result<List<ResidenceSummaryItem>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/search") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("name", name)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<List<ResidenceSummaryItem>>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de recherche : ${e.message}"))
        }
    }
}
