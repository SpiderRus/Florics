package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Запрос на добавление товара в корзину
 */
@Schema(description = "Запрос на добавление товара в корзину")
data class AddToCartRequest(
    @JsonProperty("goodsId")
    @Schema(description = "ID товара для добавления", example = "1", required = true)
    val goodsId: String,

    @JsonProperty("quantity")
    @Schema(description = "Количество единиц товара", example = "2", required = true)
    val quantity: Int
)
