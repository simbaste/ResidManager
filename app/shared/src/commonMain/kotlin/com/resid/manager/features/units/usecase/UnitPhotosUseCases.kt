package com.resid.manager.features.units.usecase

import com.resid.manager.dto.ResidencePhotoDto
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.repository.ResidenceUnitRepository
import com.resid.manager.repository.StorageRepository
import com.resid.manager.util.ImageFormatDetector

class FetchUnitPhotosUseCase(
    private val unitRepository: ResidenceUnitRepository
) {
    suspend operator fun invoke(token: String, residenceId: String, unitId: String): Result<List<ResidencePhotoDto>> {
        return unitRepository.fetchUnitPhotos(token, residenceId, unitId)
    }
}

class UpdateUnitPhotosUseCase(
    private val storageRepository: StorageRepository,
    private val unitRepository: ResidenceUnitRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        unitId: String,
        photos: List<PhotoUploadItem>
    ): Result<List<ResidencePhotoDto>> = runCatching {
        val finalPhotoDtos = mutableListOf<ResidencePhotoDto>()

        for (item in photos) {
            val photoUrl = if (item.bytes != null && item.bytes.isNotEmpty()) {
                val format = ImageFormatDetector.detect(item.bytes)
                if (format.isHeic) {
                    error("Le format HEIC n'est pas supporté. Veuillez sélectionner une image JPEG, PNG ou WebP.")
                }
                val r2Result = storageRepository.uploadUnitPhotoToR2(
                    residenceId = residenceId,
                    unitId = unitId,
                    bytes = item.bytes,
                    mimeType = format.mimeType,
                    extension = format.extension,
                    token = token
                )
                r2Result.getOrThrow()
            } else if (!item.url.isNullOrBlank()) {
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

        val updateResult = unitRepository.updateUnitPhotos(
            token = token,
            residenceId = residenceId,
            unitId = unitId,
            photos = finalPhotoDtos
        )

        updateResult.getOrThrow()
    }
}
