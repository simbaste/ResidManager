package com.resid.manager.repository

import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDraftInitRequest
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
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
import io.ktor.client.request.parameter
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
    suspend fun fetchGuarantorsByTenant(token: String, tenantId: String): Result<List<com.resid.manager.dto.GuarantorDto>>
    suspend fun initDraft(token: String, residenceUnitId: String): Result<LeaseDto>
    suspend fun updateDraftStep(token: String, leaseId: String, request: LeaseDraftStepUpdateRequest): Result<LeaseDto>
    suspend fun finalizeDraft(token: String, leaseId: String): Result<LeaseDto>
    suspend fun fetchLeaseById(token: String, residenceId: String, leaseId: String): Result<LeaseDto>
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
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la récupération des baux.")
                Result.failure(Exception(errorMsg))
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
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la création du contrat de bail.")
                Result.failure(Exception(errorMsg))
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
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de l'enregistrement du paiement.")
                Result.failure(Exception(errorMsg))
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
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la mise à jour du bail.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de mise à jour du statut : ${e.message}"))
        }
    }

    override suspend fun fetchGuarantorsByTenant(token: String, tenantId: String): Result<List<com.resid.manager.dto.GuarantorDto>> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/tenants/$tenantId/guarantors") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la récupération des garants.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur réseau lors de la récupération des garants : ${e.message}"))
        }
    }

    override suspend fun initDraft(token: String, residenceUnitId: String): Result<LeaseDto> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/leases/draft") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(LeaseDraftInitRequest(residenceUnitId))
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de l'initialisation du brouillon de bail.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur d'initialisation du brouillon : ${e.message}"))
        }
    }

    override suspend fun updateDraftStep(token: String, leaseId: String, request: LeaseDraftStepUpdateRequest): Result<LeaseDto> {
        return try {
            val response = httpClient.put("${ApiClient.BASE_URL}/api/leases/$leaseId/draft/step") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la sauvegarde de l'étape.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de sauvegarde de l'étape : ${e.message}"))
        }
    }

    override suspend fun finalizeDraft(token: String, leaseId: String): Result<LeaseDto> {
        return try {
            val response = httpClient.post("${ApiClient.BASE_URL}/api/leases/$leaseId/activate") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                Result.success(response.body())
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors de la finalisation du contrat de bail.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de finalisation du bail : ${e.message}"))
        }
    }

    override suspend fun fetchLeaseById(token: String, residenceId: String, leaseId: String): Result<LeaseDto> {
        return try {
            val response = httpClient.get("${ApiClient.BASE_URL}/api/leases") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("residenceId", residenceId)
                parameter("leaseId", leaseId)
            }
            if (response.status == HttpStatusCode.OK) {
                val leases = response.body<List<LeaseDto>>()
                val lease = leases.firstOrNull { it.id == leaseId } ?: leases.firstOrNull()
                if (lease != null) {
                    Result.success(lease)
                } else {
                    Result.failure(Exception("Contrat de bail introuvable"))
                }
            } else {
                val errorMsg = ApiClient.parseErrorMessage(response, "Erreur lors du chargement du contrat de bail.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de récupération du contrat de bail : ${e.message}"))
        }
    }
}
