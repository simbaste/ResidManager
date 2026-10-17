package com.resid.manager.repository

import com.resid.manager.dto.AvatarUploadTicketRequest
import com.resid.manager.dto.AvatarUploadTicketResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

interface StorageRepository {
    suspend fun uploadAvatarToR2(bytes: ByteArray, mimeType: String, extension: String): Result<String>
}

class StorageRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : StorageRepository {

    override suspend fun uploadAvatarToR2(bytes: ByteArray, mimeType: String, extension: String): Result<String> = runCatching {
        // 1. Obtenir le ticket d'upload présigné
        val ticket: AvatarUploadTicketResponse = httpClient.post("$baseUrl/api/auth/register/avatar-ticket") {
            contentType(ContentType.Application.Json)
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
}
