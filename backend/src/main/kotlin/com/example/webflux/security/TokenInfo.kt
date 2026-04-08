package com.example.webflux.security

import java.time.OffsetDateTime

data class TokenInfo(
    val token: String,
    val userId: String,
    val email: String,
    val roles: Set<String>,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime
)
