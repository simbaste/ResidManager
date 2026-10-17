package com.resid.manager.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.net.url.Url
import com.resid.manager.data.HttpError
import com.resid.manager.dto.AvatarUploadTicketResponse
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class R2StorageService(
    private val config: StorageConfig = StorageConfig.fromEnv()
) {
    private val allowedMimeTypes = mapOf(
        "image/jpeg" to listOf("jpg", "jpeg"),
        "image/png" to listOf("png"),
        "image/webp" to listOf("webp")
    )

    private val client: S3Client by lazy {
        S3Client {
            region = "auto"
            endpointUrl = Url.parse(config.endpoint)
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = config.accessKey
                secretAccessKey = config.secretKey
            }
        }
    }

    suspend fun generateAvatarUploadTicket(extension: String, mimeType: String): AvatarUploadTicketResponse {
        val normalizedMimeType = mimeType.trim().lowercase()
        val normalizedExtension = extension.trim().lowercase().removePrefix(".")

        val allowedExtensionsForMime = allowedMimeTypes[normalizedMimeType]
        if (allowedExtensionsForMime == null || !allowedExtensionsForMime.contains(normalizedExtension)) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Format d'image non supporté. Types acceptés : image/jpeg (.jpg, .jpeg), image/png (.png), image/webp (.webp)."
            )
        }

        val key = "users/avatars/${UUID.randomUUID()}.$normalizedExtension"
        val putRequest = PutObjectRequest {
            bucket = config.bucketName
            this.key = key
            contentType = normalizedMimeType
        }

        val presignedUrl = client.presignPutObject(putRequest, 300.seconds)

        val uploadUrl = presignedUrl.url.toString()
        val publicUrl = "${config.publicDomain}/$key"

        return AvatarUploadTicketResponse(
            uploadUrl = uploadUrl,
            publicUrl = publicUrl,
            expiresInSeconds = 300
        )
    }
}
