package com.example.webflux.model

import java.time.Instant

data class Review(
    val id: String,
    val plantId: String,
    val userId: Long,
    val userName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
