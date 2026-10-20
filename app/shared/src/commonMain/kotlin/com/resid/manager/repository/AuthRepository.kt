package com.resid.manager.repository

import com.resid.manager.dto.AuthRequest
import com.resid.manager.dto.AuthResponse
import com.resid.manager.dto.RegisterRequest
import com.resid.manager.network.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface AuthRepository {
    suspend fun login(email: String, passwordPlain: String): Result<AuthResponse>
    suspend fun register(
        firstName: String,
        lastName: String,
        birthDate: String?,
        phone: String?,
        email: String,
        passwordPlain: String,
        avatarUrl: String? = null
    ): Result<AuthResponse>
}

class AuthRepositoryImpl(
    private val httpClient: HttpClient
) : AuthRepository {
    override suspend fun login(email: String, passwordPlain: String): Result<AuthResponse> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, passwordPlain))
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<AuthResponse>())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Identifiants ou paramètres incorrects.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de connexion : ${e.message}"))
        }
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        birthDate: String?,
        phone: String?,
        email: String,
        passwordPlain: String,
        avatarUrl: String?
    ): Result<AuthResponse> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(firstName, lastName, birthDate, phone, email, passwordPlain, avatarUrl))
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body<AuthResponse>())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Une erreur est survenue lors de l'inscription.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur d'inscription : ${e.message}"))
        }
    }
}
