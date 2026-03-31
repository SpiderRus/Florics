package com.example.webflux.domain.model

import java.time.Instant

data class Review(
    val id: String,
    val goodsId: String,
    val userId: Long,
    val userName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
