package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Запрос на синхронизацию локальной корзины с серверной
 */
@Schema(description = "Запрос на объединение локальной корзины (из localStorage) с серверной")
data class MergeCartRequest(
    @JsonProperty("items")
    @Schema(description = "Товары из локальной корзины для синхронизации")
    val items: List<LocalCartItem>
)

/**
 * Элемент локальной корзины (только goodsId + quantity)
 */
@Schema(description = "Элемент локальной корзины из localStorage")
data class LocalCartItem(
    @JsonProperty("goodsId")
    @Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @JsonProperty("quantity")
    @Schema(description = "Количество единиц", example = "2")
    val quantity: Int
)
