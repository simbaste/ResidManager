package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDraftInitRequest
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class DraftLeaseUseCases {
    suspend fun initDraft(token: String, request: LeaseDraftInitRequest): Result<LeaseDto> {
        return try {
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/leases/draft") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Erreur serveur : ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDraftStep(token: String, leaseId: String, request: LeaseDraftStepUpdateRequest): Result<LeaseDto> {
        return try {
            val response = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/leases/$leaseId/draft/step") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Erreur serveur : ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun finalizeDraft(token: String, leaseId: String): Result<LeaseDto> {
        return try {
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/leases/$leaseId/activate") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Erreur serveur : ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
