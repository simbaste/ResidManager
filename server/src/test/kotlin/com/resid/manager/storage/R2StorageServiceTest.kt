package com.resid.manager.storage

import com.resid.manager.data.HttpError
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class R2StorageServiceTest {

    private val testConfig = StorageConfig(
        accessKey = "test-access-key",
        secretKey = "test-secret-key",
        endpoint = "https://18ff4c5210a4c6b597413dd0672aec7f.r2.cloudflarestorage.com",
        bucketName = "residmanager-media-test",
        publicDomain = "https://media-test.residmanager.com"
    )

    private val service = R2StorageService(testConfig)

    @Test
    fun testGenerateAvatarUploadTicket_success() = runBlocking {
        val ticket = service.generateAvatarUploadTicket("jpg", "image/jpeg")
        assertEquals(300, ticket.expiresInSeconds)
        assertTrue(ticket.publicUrl.startsWith("https://media-test.residmanager.com/users/avatars/"))
        assertTrue(ticket.publicUrl.endsWith(".jpg"))
        assertTrue(ticket.uploadUrl.contains("residmanager-media-test"))
        assertTrue(ticket.uploadUrl.contains("users/avatars/"))
    }

    @Test
    fun testGenerateAvatarUploadTicket_invalidMimeType() {
        assertFailsWith<HttpError> {
            runBlocking {
                service.generateAvatarUploadTicket("pdf", "application/pdf")
            }
        }
    }

    @Test
    fun testGenerateAvatarUploadTicket_mismatchedExtension() {
        assertFailsWith<HttpError> {
            runBlocking {
                service.generateAvatarUploadTicket("png", "image/jpeg")
            }
        }
    }
}
