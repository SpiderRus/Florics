package com.example.webflux.security

import com.example.webflux.domain.model.UserRole
import java.time.OffsetDateTime

data class TokenInfo(
    val token: String,
    val userId: String,
    val email: String,
    val roles: Set<UserRole>,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime
)
