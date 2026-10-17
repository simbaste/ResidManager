package com.resid.manager.storage

data class StorageConfig(
    val accessKey: String,
    val secretKey: String,
    val endpoint: String,
    val bucketName: String,
    val publicDomain: String
) {
    companion object {
        fun fromEnv(): StorageConfig {
            val accessKey = System.getenv("R2_ACCESS_KEY") ?: ""
            val secretKey = System.getenv("R2_SECRET_KEY") ?: ""
            val endpoint = System.getenv("R2_ENDPOINT")
                ?.takeIf { it.isNotBlank() }
                ?: "https://18ff4c5210a4c6b597413dd0672aec7f.r2.cloudflarestorage.com"
            val bucketName = System.getenv("R2_BUCKET_NAME")
                ?.takeIf { it.isNotBlank() }
                ?: "residmanager-media-dev"
            val publicDomain = System.getenv("R2_PUBLIC_DOMAIN")
                ?.takeIf { it.isNotBlank() }
                ?.trimEnd('/')
                ?: "https://pub-xxxx.r2.dev"

            return StorageConfig(
                accessKey = accessKey,
                secretKey = secretKey,
                endpoint = endpoint,
                bucketName = bucketName,
                publicDomain = publicDomain
            )
        }
    }
}
