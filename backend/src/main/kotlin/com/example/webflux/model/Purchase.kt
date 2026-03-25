package com.example.webflux.model

import java.time.Instant

data class Purchase(
    val id: String,
    val userId: Long,
    val plantId: String,
    val price: Double,
    val purchaseDate: Instant,
    val quantity: Int = 1
)
