package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * Запрос на изменение количества товара в корзине
 */
@Schema(description = "Запрос на изменение количества товара")
data class UpdateQuantityRequest(
    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    @field:Max(value = 99, message = "Количество не может превышать 99")
    @JsonProperty("quantity")
    @Schema(description = "Новое количество единиц товара (1-99)", example = "3", required = true)
    val quantity: Int
)
