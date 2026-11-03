package com.resid.manager.repository

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceDirectoryDTO
import com.resid.manager.dto.ResidenceSummaryItemDto
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

interface ResidenceRepository {
    suspend fun fetchResidences(token: String): Result<ResidenceDirectoryDTO>
    suspend fun createResidence(token: String, request: ResidenceCreateRequest): Result<ResidenceSummaryItemDto>
    suspend fun searchResidences(token: String, name: String): Result<List<ResidenceSummaryItemDto>>
    suspend fun updateResidence(token: String, residenceId: String, request: ResidenceCreateRequest): Result<ResidenceSummaryItemDto>
    suspend fun updateResidencePhoto(token: String, residenceId: String, photoUrl: String): Result<ResidenceSummaryItemDto>
    suspend fun fetchResidencePhotos(token: String, residenceId: String): Result<List<com.resid.manager.dto.ResidencePhotoDto>>
    suspend fun updateResidencePhotos(token: String, residenceId: String, photos: List<com.resid.manager.dto.ResidencePhotoDto>): Result<List<com.resid.manager.dto.ResidencePhotoDto>>
    suspend fun deleteResidence(token: String, residenceId: String): Result<Unit>
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

    override suspend fun createResidence(token: String, request: ResidenceCreateRequest): Result<ResidenceSummaryItemDto> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/residences") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceSummaryItemDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de création : ${e.message}"))
        }
    }

    override suspend fun searchResidences(token: String, name: String): Result<List<ResidenceSummaryItemDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/search") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("name", name)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<List<ResidenceSummaryItemDto>>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de recherche : ${e.message}"))
        }
    }

    override suspend fun updateResidence(
        token: String,
        residenceId: String,
        request: ResidenceCreateRequest
    ): Result<ResidenceSummaryItemDto> {
        return try {
            val response = httpClient.put("${ApiClient.BASE_URL}/api/residences/$residenceId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceSummaryItemDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de modification : ${e.message}"))
        }
    }

    override suspend fun updateResidencePhoto(
        token: String,
        residenceId: String,
        photoUrl: String
    ): Result<ResidenceSummaryItemDto> {
        return try {
            val response = httpClient.put("${ApiClient.BASE_URL}/api/residences/$residenceId/photo") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(com.resid.manager.dto.ResidencePhotoUpdateRequest(photoUrl = photoUrl))
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<ResidenceSummaryItemDto>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de mise à jour de la photo : ${e.message}"))
        }
    }

    override suspend fun fetchResidencePhotos(
        token: String,
        residenceId: String
    ): Result<List<com.resid.manager.dto.ResidencePhotoDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/photos") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<List<com.resid.manager.dto.ResidencePhotoDto>>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur lors de la récupération des photos : ${e.message}"))
        }
    }

    override suspend fun updateResidencePhotos(
        token: String,
        residenceId: String,
        photos: List<com.resid.manager.dto.ResidencePhotoDto>
    ): Result<List<com.resid.manager.dto.ResidencePhotoDto>> {
        return try {
            val response = httpClient.put("${ApiClient.BASE_URL}/api/residences/$residenceId/photos") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(com.resid.manager.dto.ResidencePhotosUpdateRequest(photos = photos))
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<List<com.resid.manager.dto.ResidencePhotoDto>>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur lors de la mise à jour des photos : ${e.message}"))
        }
    }

    override suspend fun deleteResidence(token: String, residenceId: String): Result<Unit> {
        return try {
            val response = httpClient.delete("${ApiClient.BASE_URL}/api/residences/$residenceId") {
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
}
