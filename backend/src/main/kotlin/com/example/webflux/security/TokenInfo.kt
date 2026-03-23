package com.example.webflux.security

import java.time.Instant

data class TokenInfo(
    val token: String,
    val userId: Long,
    val email: String,
    val roles: Set<String>,
    val createdAt: Instant,
    val expiresAt: Instant
)
