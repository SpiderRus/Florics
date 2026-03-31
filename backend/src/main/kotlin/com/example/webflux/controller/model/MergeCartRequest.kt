package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Запрос на синхронизацию локальной корзины с серверной
 */
@Schema(description = "Запрос на объединение локальной корзины (из localStorage) с серверной")
data class MergeCartRequest(
    @field:Valid
    @field:NotNull(message = "Список товаров не может быть null")
    @JsonProperty("items")
    @Schema(description = "Товары из локальной корзины для синхронизации")
    val items: List<LocalCartItem>
)

/**
 * Элемент локальной корзины (только goodsId + quantity)
 */
@Schema(description = "Элемент локальной корзины из localStorage")
data class LocalCartItem(
    @field:NotBlank(message = "ID товара обязателен")
    @JsonProperty("goodsId")
    @Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    @field:Max(value = 99, message = "Количество не может превышать 99")
    @JsonProperty("quantity")
    @Schema(description = "Количество единиц (1-99)", example = "2")
    val quantity: Int
)
