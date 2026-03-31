package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Информация о покупке")
data class PurchaseDto(
    @Schema(description = "ID покупки")
    val id: String,

    @Schema(description = "ID товара")
    val goodsId: String,

    @Schema(description = "Цена покупки")
    val price: Double,

    @Schema(description = "Дата покупки")
    val purchaseDate: Instant,

    @Schema(description = "Количество")
    val quantity: Int
)
