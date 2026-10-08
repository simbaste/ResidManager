package com.resid.manager.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    const val secret = "resid-manager-very-secure-jwt-secret-key-12345678" // In production, read from env
    const val issuer = "com.resid.manager"
    const val audience = "com.resid.manager.audience"
    const val realm = "com.resid.manager"
    private const val validityInMs = 36_000_000 // 10 hours
    private const val refreshTokenValidityInMs = 30L * 24 * 60 * 60 * 1000 // 30 days

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: String, email: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: String, email: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenValidityInMs))
            .sign(algorithm)
    }

    /**
     * Verifies that the provided token is a valid refresh token.
     * Returns the pair of (userId, email) if valid, null otherwise.
     */
    fun verifyRefreshToken(token: String): Pair<String, String>? {
        return try {
            val decoded = verifier.verify(token)
            val type = decoded.getClaim("type").asString()
            if (type != "refresh") {
                return null
            }
            val userId = decoded.getClaim("userId").asString() ?: return null
            val email = decoded.getClaim("email").asString() ?: return null
            userId to email
        } catch (_: Exception) {
            null
        }
    }
}
