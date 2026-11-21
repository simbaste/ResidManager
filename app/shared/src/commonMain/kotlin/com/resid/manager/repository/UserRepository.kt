package com.resid.manager.repository

import com.resid.manager.dto.UserSearchDto
import com.resid.manager.network.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

interface UserRepository {
    suspend fun searchUsers(token: String, query: String, residenceId: String? = null): Result<List<UserSearchDto>>
}

class UserRepositoryImpl(
    private val httpClient: HttpClient
) : UserRepository {
    override suspend fun searchUsers(
        token: String,
        query: String,
        residenceId: String?
    ): Result<List<UserSearchDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                parameter("q", query)
                if (residenceId != null) {
                    parameter("residenceId", residenceId)
                }
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la recherche des utilisateurs.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de recherche des utilisateurs : ${e.message}"))
        }
    }
}
