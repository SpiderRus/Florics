package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Сводка корзины с расчётом итоговых значений
 */
@Schema(description = "Сводка корзины покупок")
data class CartSummaryDto(
    @JsonProperty("items")
    @Schema(description = "Список товаров в корзине")
    val items: List<CartItemDto>,

    @JsonProperty("totalItems")
    @Schema(description = "Общее количество товаров (сумма всех quantity)", example = "5")
    val totalItems: Int,

    @JsonProperty("totalPrice")
    @Schema(description = "Итоговая стоимость корзины", example = "4500.00")
    val totalPrice: Double
)
