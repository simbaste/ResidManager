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
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }
}
