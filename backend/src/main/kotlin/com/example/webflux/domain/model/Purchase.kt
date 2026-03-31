package com.example.webflux.domain.model

import java.time.Instant

data class Purchase(
    val id: String,
    val userId: Long,
    val goodsId: String,
    val price: Double,
    val purchaseDate: Instant,
    val quantity: Int = 1
)
