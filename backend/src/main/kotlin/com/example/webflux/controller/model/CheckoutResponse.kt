package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Ответ на оформление заказа")
data class CheckoutResponse(
    @Schema(description = "ID заказа", example = "550e8400-e29b-41d4-a716-446655440000")
    val orderId: String,

    @Schema(description = "Общая стоимость заказа", example = "5400.0")
    val totalPrice: Double,

    @Schema(description = "Список купленных товаров")
    val items: List<PurchasedItem>,

    @Schema(description = "Дата и время покупки")
    val purchaseDate: Instant
)

@Schema(description = "Купленный товар")
data class PurchasedItem(
    @Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @Schema(description = "Название товара", example = "Монстера деликатесная")
    val goodsName: String,

    @Schema(description = "Количество", example = "2")
    val quantity: Int,

    @Schema(description = "Цена за единицу", example = "1500.0")
    val price: Double
)
