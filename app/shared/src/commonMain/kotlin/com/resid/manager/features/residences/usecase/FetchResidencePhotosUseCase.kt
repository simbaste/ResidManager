package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.ResidencePhotoDto
import com.resid.manager.repository.ResidenceRepository

class FetchResidencePhotosUseCase(
    private val residenceRepository: ResidenceRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<ResidencePhotoDto>> {
        return residenceRepository.fetchResidencePhotos(token, residenceId)
    }
}
