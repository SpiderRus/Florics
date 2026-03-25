package com.example.webflux.model

import java.time.Instant

/**
 * Внутренняя модель элемента корзины для хранения
 */
data class CartItem(
    val id: String,              // UUID строки корзины
    val userId: Long,            // Привязка к пользователю
    val plantId: String,         // ID растения
    val quantity: Int,           // Количество
    val addedAt: Instant         // Время добавления
)

