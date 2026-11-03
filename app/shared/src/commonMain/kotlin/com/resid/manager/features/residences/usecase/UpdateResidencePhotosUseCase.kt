package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.ResidencePhotoDto
import com.resid.manager.repository.ResidenceRepository
import com.resid.manager.repository.StorageRepository
import com.resid.manager.util.ImageFormatDetector

data class PhotoUploadItem(
    val url: String? = null,
    val bytes: ByteArray? = null,
    val description: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PhotoUploadItem

        if (url != other.url) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url?.hashCode() ?: 0
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + description.hashCode()
        return result
    }
}

class UpdateResidencePhotosUseCase(
    private val storageRepository: StorageRepository,
    private val residenceRepository: ResidenceRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        photos: List<PhotoUploadItem>
    ): Result<List<ResidencePhotoDto>> = runCatching {
        val finalPhotoDtos = mutableListOf<ResidencePhotoDto>()

        for (item in photos) {
            val photoUrl = if (item.bytes != null && item.bytes.isNotEmpty()) {
                // Nouvelle image : upload vers Cloudflare R2
                val format = ImageFormatDetector.detect(item.bytes)
                if (format.isHeic) {
                    error("Le format HEIC n'est pas supporté. Veuillez sélectionner une image JPEG, PNG ou WebP.")
                }
                val r2Result = storageRepository.uploadResidencePhotoToR2(
                    residenceId = residenceId,
                    bytes = item.bytes,
                    mimeType = format.mimeType,
                    extension = format.extension,
                    token = token
                )
                r2Result.getOrThrow()
            } else if (!item.url.isNullOrBlank()) {
                // Image existante déjà sur R2
                item.url
            } else {
                null
            }

            if (photoUrl != null) {
                finalPhotoDtos.add(
                    ResidencePhotoDto(
                        url = photoUrl,
                        description = item.description
                    )
                )
            }
        }

        // Envoi de la liste complète vers l'API Backend
        val updateResult = residenceRepository.updateResidencePhotos(
            token = token,
            residenceId = residenceId,
            photos = finalPhotoDtos
        )

        updateResult.getOrThrow()
    }
}
