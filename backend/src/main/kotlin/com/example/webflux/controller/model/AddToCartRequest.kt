package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * Запрос на добавление товара в корзину
 */
@Schema(description = "Запрос на добавление товара в корзину")
data class AddToCartRequest(
    @field:NotBlank(message = "ID товара обязателен")
    @JsonProperty("goodsId")
    @Schema(description = "ID товара для добавления", example = "1", required = true)
    val goodsId: String,

    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    @field:Max(value = 99, message = "Количество не может превышать 99")
    @JsonProperty("quantity")
    @Schema(description = "Количество единиц товара (1-99)", example = "2", required = true)
    val quantity: Int
)
