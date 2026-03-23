package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Модель комнатного растения")
data class Plant(
    @Schema(description = "Уникальный идентификатор растения", example = "1")
    val id: String,

    @Schema(description = "Название растения", example = "Монстера деликатесная")
    val name: String,

    @Schema(description = "Краткое описание растения", example = "Популярная тропическая лиана с крупными резными листьями")
    val description: String,

    @Schema(description = "Цена в рублях", example = "1500.0")
    val price: Double,

    @Schema(description = "Список URL изображений растения")
    val images: List<String>,

    @Schema(description = "Категория растения", example = "Лианы")
    val category: String,

    @Schema(description = "Уровень сложности ухода", example = "Легко", allowableValues = ["Легко", "Средне", "Сложно"])
    val difficulty: String
)
