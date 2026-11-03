package com.resid.manager.repository

import com.resid.manager.dto.AvatarUploadTicketRequest
import com.resid.manager.dto.AvatarUploadTicketResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

interface StorageRepository {
    suspend fun uploadAvatarToR2(bytes: ByteArray, mimeType: String, extension: String): Result<String>
    suspend fun uploadResidencePhotoToR2(residenceId: String, bytes: ByteArray, mimeType: String, extension: String, token: String): Result<String>
    suspend fun uploadUnitPhotoToR2(residenceId: String, unitId: String, bytes: ByteArray, mimeType: String, extension: String, token: String): Result<String>
}

class StorageRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : StorageRepository {

    override suspend fun uploadAvatarToR2(bytes: ByteArray, mimeType: String, extension: String): Result<String> = runCatching {
        // 1. Obtenir le ticket d'upload présigné
        val ticket: AvatarUploadTicketResponse = httpClient.post("$baseUrl/api/auth/register/avatar-ticket") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            setBody(AvatarUploadTicketRequest(mimeType = mimeType, extension = extension))
        }.body()

        // 2. Upload direct du binaire sur Cloudflare R2
        val uploadResponse = httpClient.put(ticket.uploadUrl) {
            setBody(bytes)
            header(HttpHeaders.ContentType, mimeType)
        }

        if (!uploadResponse.status.isSuccess()) {
            error("Échec de l'upload de l'image (code: ${uploadResponse.status.value})")
        }

        ticket.publicUrl
    }

    override suspend fun uploadResidencePhotoToR2(
        residenceId: String,
        bytes: ByteArray,
        mimeType: String,
        extension: String,
        token: String
    ): Result<String> = runCatching {
        // 1. Obtenir le ticket d'upload présigné pour la résidence
        val ticket: AvatarUploadTicketResponse = httpClient.post("$baseUrl/api/residences/$residenceId/photo-ticket") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(AvatarUploadTicketRequest(mimeType = mimeType, extension = extension))
        }.body()

        // 2. Upload direct du binaire sur Cloudflare R2
        val uploadResponse = httpClient.put(ticket.uploadUrl) {
            setBody(bytes)
            header(HttpHeaders.ContentType, mimeType)
        }

        if (!uploadResponse.status.isSuccess()) {
            error("Échec de l'upload de l'image de résidence (code: ${uploadResponse.status.value})")
        }

        ticket.publicUrl
    }

    override suspend fun uploadUnitPhotoToR2(
        residenceId: String,
        unitId: String,
        bytes: ByteArray,
        mimeType: String,
        extension: String,
        token: String
    ): Result<String> = runCatching {
        // 1. Obtenir le ticket d'upload présigné pour le logement
        val ticket: AvatarUploadTicketResponse = httpClient.post("$baseUrl/api/units/$unitId/photo-ticket?residenceId=$residenceId") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(AvatarUploadTicketRequest(mimeType = mimeType, extension = extension))
        }.body()

        // 2. Upload direct du binaire sur Cloudflare R2
        val uploadResponse = httpClient.put(ticket.uploadUrl) {
            setBody(bytes)
            header(HttpHeaders.ContentType, mimeType)
        }

        if (!uploadResponse.status.isSuccess()) {
            error("Échec de l'upload de l'image du logement (code: ${uploadResponse.status.value})")
        }

        ticket.publicUrl
    }
}
