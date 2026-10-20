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
            email = "jean.dupont@test.com",
            passwordPlain = "Password123!",
            avatarBytes = null
        )

        assertTrue(result.isSuccess)
        assertEquals(0, storageRepo.uploadCallCount)
        assertEquals(1, authRepo.registerCallCount)
        assertNull(authRepo.registeredAvatarUrl)
    }

    @Test
    fun register_withAvatar_uploadsToR2AndPassesUrlToRepository() = runTest {
        val authRepo = FakeAuthRepository()
        val expectedUrl = "https://media.residmanager.com/users/avatars/avatar-123.jpg"
        val storageRepo = FakeStorageRepository(returnedUrl = expectedUrl)
        val useCase = RegisterUseCase(authRepo, storageRepo)

        val dummyBytes = byteArrayOf(1, 2, 3, 4)
        val result = useCase(
            firstName = "Jean",
            lastName = "Dupont",
            birthDate = null,
            phone = null,
            email = "jean.dupont@test.com",
            passwordPlain = "Password123!",
            avatarBytes = dummyBytes
        )

        assertTrue(result.isSuccess)
        assertEquals(1, storageRepo.uploadCallCount)
        assertEquals(1, authRepo.registerCallCount)
        assertEquals(expectedUrl, authRepo.registeredAvatarUrl)
    }

    @Test
    fun register_whenStorageFails_abortsRegistration() = runTest {
        val authRepo = FakeAuthRepository()
        val storageRepo = FakeStorageRepository(shouldFail = true)
        val useCase = RegisterUseCase(authRepo, storageRepo)

        val dummyBytes = byteArrayOf(1, 2, 3, 4)
        val result = useCase(
            firstName = "Jean",
            lastName = "Dupont",
            birthDate = null,
            phone = null,
            email = "jean.dupont@test.com",
            passwordPlain = "Password123!",
            avatarBytes = dummyBytes
        )

        assertTrue(result.isFailure)
        assertEquals(1, storageRepo.uploadCallCount)
        assertEquals(0, authRepo.registerCallCount)
    }
}
