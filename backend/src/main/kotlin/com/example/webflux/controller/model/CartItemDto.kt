package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * DTO для элемента корзины с полной информацией о товаре
 */
@Schema(description = "Элемент корзины с информацией о товаре")
data class CartItemDto(
    @JsonProperty("id")
    @Schema(description = "ID элемента корзины", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @JsonProperty("goods")
    @Schema(description = "Полная информация о товаре")
    val goods: GoodsDto,

    @JsonProperty("quantity")
    @Schema(description = "Количество единиц товара", example = "2")
    val quantity: Int,

    @JsonProperty("addedAt")
    @Schema(description = "Время добавления в корзину")
    val addedAt: Instant
)
