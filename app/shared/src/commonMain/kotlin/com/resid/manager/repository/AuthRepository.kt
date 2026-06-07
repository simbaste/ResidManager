package com.resid.manager.repository

import com.resid.manager.dto.AuthRequest
import com.resid.manager.dto.AuthResponse
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.RegisterRequest
import com.resid.manager.network.ApiClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface AuthRepository {
    suspend fun login(email: String, passwordPlain: String): Result<AuthResponse>
    suspend fun register(
        firstName: String,
        lastName: String,
        birthDate: String?,
        phone: String?,
        email: String,
        passwordPlain: String
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
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
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
        passwordPlain: String
    ): Result<AuthResponse> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(firstName, lastName, birthDate, phone, email, passwordPlain))
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body<AuthResponse>())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur d'inscription : ${e.message}"))
        }
    }
}
