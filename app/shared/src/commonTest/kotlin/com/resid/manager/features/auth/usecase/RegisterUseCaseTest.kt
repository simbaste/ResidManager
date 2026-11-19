package com.resid.manager.features.auth.usecase

import com.resid.manager.dto.AuthResponse
import com.resid.manager.dto.UserDto
import com.resid.manager.repository.AuthRepository
import com.resid.manager.repository.StorageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        var registeredAvatarUrl: String? = null
        var registerCallCount = 0

        override suspend fun login(email: String, passwordPlain: String): Result<AuthResponse> {
            error("Not implemented")
        }

        override suspend fun register(
            firstName: String,
            lastName: String,
            birthDate: String?,
            phone: String?,
            email: String,
            passwordPlain: String,
            avatarUrl: String?
        ): Result<AuthResponse> {
            registerCallCount++
            registeredAvatarUrl = avatarUrl
            return Result.success(
                AuthResponse(
                    token = "fake-token",
                    refreshToken = "fake-refresh-token",
                    user = UserDto(
                        id = "user-123",
                        email = email,
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        birthDate = birthDate,
                        createdAt = "2026-10-18T00:00:00Z",
                        updatedAt = "2026-10-18T00:00:00Z"
                    )
                )
            )
        }
    }

    private class FakeStorageRepository(
        private val shouldFail: Boolean = false,
        private val returnedUrl: String = "https://media.residmanager.com/users/avatars/test.jpg"
    ) : StorageRepository {
        var uploadCallCount = 0

        override suspend fun uploadAvatarToR2(
            bytes: ByteArray,
            mimeType: String,
            extension: String
        ): Result<String> {
            uploadCallCount++
            return if (shouldFail) {
                Result.failure(Exception("R2 upload failure"))
            } else {
                Result.success(returnedUrl)
            }
        }

        override suspend fun uploadResidencePhotoToR2(residenceId: String, bytes: ByteArray, mimeType: String, extension: String, token: String): Result<String> {
            return Result.success(returnedUrl)
        }

        override suspend fun uploadUnitPhotoToR2(residenceId: String, unitId: String, bytes: ByteArray, mimeType: String, extension: String, token: String): Result<String> {
            return Result.success(returnedUrl)
        }
    }

    @Test
    fun register_withoutAvatar_succeedsWithNullAvatarUrl() = runTest {
        val authRepo = FakeAuthRepository()
        val storageRepo = FakeStorageRepository()
        val useCase = RegisterUseCase(authRepo, storageRepo)

        val result = useCase(
            firstName = "Jean",
            lastName = "Dupont",
            birthDate = null,
            phone = null,
            email = "jean@example.com",
            passwordPlain = "password12345",
            avatarBytes = null,
            avatarMimeType = "image/jpeg",
            avatarExtension = "jpg"
        )

        assertTrue(result.isSuccess)
        assertEquals(1, authRepo.registerCallCount)
        assertEquals(0, storageRepo.uploadCallCount)
        assertNull(authRepo.registeredAvatarUrl)
    }

    @Test
    fun register_withAvatar_uploadsAndRegistersWithUrl() = runTest {
        val authRepo = FakeAuthRepository()
        val storageRepo = FakeStorageRepository()
        val useCase = RegisterUseCase(authRepo, storageRepo)

        val fakeBytes = byteArrayOf(1, 2, 3)
        val result = useCase(
            firstName = "Marie",
            lastName = "Curie",
            birthDate = "1867-11-07",
            phone = "123456789",
            email = "marie@example.com",
            passwordPlain = "password12345",
            avatarBytes = fakeBytes,
            avatarMimeType = "image/jpeg",
            avatarExtension = "jpg"
        )

        assertTrue(result.isSuccess)
        assertEquals(1, storageRepo.uploadCallCount)
        assertEquals(1, authRepo.registerCallCount)
        assertEquals("https://media.residmanager.com/users/avatars/test.jpg", authRepo.registeredAvatarUrl)
    }

    @Test
    fun register_withAvatar_uploadFails_fallsBackToNullAvatar() = runTest {
        val authRepo = FakeAuthRepository()
        val storageRepo = FakeStorageRepository(shouldFail = true)
        val useCase = RegisterUseCase(authRepo, storageRepo)

        val fakeBytes = byteArrayOf(1, 2, 3)
        val result = useCase(
            firstName = "Albert",
            lastName = "Einstein",
            birthDate = null,
            phone = null,
            email = "albert@example.com",
            passwordPlain = "password12345",
            avatarBytes = fakeBytes,
            avatarMimeType = "image/png",
            avatarExtension = "png"
        )

        assertTrue(result.isFailure)
        assertEquals(1, storageRepo.uploadCallCount)
        assertEquals(0, authRepo.registerCallCount)
    }
}
