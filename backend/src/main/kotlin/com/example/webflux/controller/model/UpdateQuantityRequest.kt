package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Запрос на изменение количества товара в корзине
 */
@Schema(description = "Запрос на изменение количества товара")
data class UpdateQuantityRequest(
    @JsonProperty("quantity")
    @Schema(description = "Новое количество единиц товара", example = "3", required = true)
    val quantity: Int
)
