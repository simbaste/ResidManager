package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.ApplicationRequest
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.RoleDto
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class JoinResidenceUseCase {
    suspend operator fun invoke(token: String, userId: String, residenceId: String): Result<Unit> {
        if (token.isBlank() || userId.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres de demande manquants"))
        }

        return try {
            val req = ApplicationRequest(role = RoleDto.TENANT)
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/applications?residenceId=$residenceId&userId=$userId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(req)
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
