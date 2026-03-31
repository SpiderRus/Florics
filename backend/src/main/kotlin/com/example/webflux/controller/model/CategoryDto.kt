package com.example.webflux.controller.model

import com.example.webflux.domain.model.GoodsType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Модель категории товаров для API")
data class CategoryDto(
    @Schema(description = "Уникальный идентификатор категории", example = "1")
    val id: String,

    @Schema(description = "Название категории", example = "Лианы")
    val name: String,

    @Schema(description = "Тип товаров в категории", example = "PHYSICAL")
    val type: GoodsType
)
