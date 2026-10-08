package com.resid.manager.repository

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeasePaymentRequest
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.LeaseUpdateRequest
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.network.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface LeaseRepository {
    suspend fun fetchLeases(token: String, residenceId: String): Result<List<LeaseDto>>
    suspend fun createLease(token: String, residenceUnitId: String, request: LeaseCreateRequest): Result<LeaseDto>
    suspend fun recordLeasePayment(token: String, leaseId: String, amount: Double, category: String): Result<LeaseDto>
    suspend fun updateLeaseStatus(token: String, leaseId: String, status: LeaseStatusDto): Result<LeaseDto>
}

class LeaseRepositoryImpl(
    private val httpClient: HttpClient
) : LeaseRepository {
    override suspend fun fetchLeases(token: String, residenceId: String): Result<List<LeaseDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/baux") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de récupération des baux : ${e.message}"))
        }
    }

    override suspend fun createLease(
        token: String,
        residenceUnitId: String,
        request: LeaseCreateRequest
    ): Result<LeaseDto> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/logements/$residenceUnitId/baux") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de création de bail : ${e.message}"))
        }
    }

    override suspend fun recordLeasePayment(token: String, leaseId: String, amount: Double, category: String): Result<LeaseDto> {
        return try {
            val mappedCategory = when (category.uppercase()) {
                "CAUTION", "DEPOSIT" -> TransactionCategoryDto.DEPOSIT
                else -> TransactionCategoryDto.RENT
            }
            val req = LeasePaymentRequest(amountPaid = amount, category = mappedCategory)
            val response = httpClient.put("${ApiClient.BASE_URL}/api/leases/$leaseId/payment") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(req)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur d'enregistrement du versement : ${e.message}"))
        }
    }

    override suspend fun updateLeaseStatus(token: String, leaseId: String, status: LeaseStatusDto): Result<LeaseDto> {
        return try {
            val req = LeaseUpdateRequest(status = status)
            val response = httpClient.put("${ApiClient.BASE_URL}/api/leases/$leaseId/status") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(req)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorBody = response.body<ErrorResponse>()
                Result.failure(Exception(errorBody.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de mise à jour du statut : ${e.message}"))
        }
    }
}
