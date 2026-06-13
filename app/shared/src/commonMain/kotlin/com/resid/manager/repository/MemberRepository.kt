package com.resid.manager.repository

import com.resid.manager.dto.*
import com.resid.manager.network.ApiClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface MemberRepository {
    suspend fun fetchMembers(token: String, residenceId: String): Result<List<ResidenceMemberSummary>>
    suspend fun inviteMember(token: String, residenceId: String, email: String, role: String): Result<Unit>
    suspend fun updateMemberStatus(token: String, residenceId: String, targetUserId: String, status: String, role: String?): Result<Unit>
}

class MemberRepositoryImpl(
    private val httpClient: HttpClient
) : MemberRepository {
    override suspend fun fetchMembers(token: String, residenceId: String): Result<List<ResidenceMemberSummary>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/members") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de récupération des membres : ${e.message}"))
        }
    }

    override suspend fun inviteMember(token: String, residenceId: String, email: String, role: String): Result<Unit> {
        return try {
            val req = InviteMemberRequest(email = email, role = role)
            val response = httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/members/invite") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(req)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur d'invitation : ${e.message}"))
        }
    }

    override suspend fun updateMemberStatus(
        token: String,
        residenceId: String,
        targetUserId: String,
        status: String,
        role: String?
    ): Result<Unit> {
        return try {
            val req = MemberStatusUpdateRequest(status = status, role = role)
            val response = httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/members/$targetUserId/status") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(req)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de modification du membre : ${e.message}"))
        }
    }
}
