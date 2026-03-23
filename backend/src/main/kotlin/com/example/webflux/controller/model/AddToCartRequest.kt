package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Запрос на добавление товара в корзину
 */
@Schema(description = "Запрос на добавление товара в корзину")
data class AddToCartRequest(
    @JsonProperty("plantId")
    @Schema(description = "ID растения для добавления", example = "1", required = true)
    val plantId: String,

    @JsonProperty("quantity")
    @Schema(description = "Количество единиц товара", example = "2", required = true)
    val quantity: Int
)
