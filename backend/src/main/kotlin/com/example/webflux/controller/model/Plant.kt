package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Модель товара (растение или мастер-класс)")
data class Plant(
    @Schema(description = "Уникальный идентификатор", example = "1")
    val id: String,

    @Schema(description = "Название", example = "Монстера деликатесная")
    val name: String,

    @Schema(description = "Краткое описание", example = "Популярная тропическая лиана с крупными резными листьями")
    val description: String,

    @Schema(description = "Цена в рублях", example = "1500.0")
    val price: Double,

    @Schema(description = "Список URL изображений")
    val images: List<String>,

    @Schema(description = "Категория", example = "Лианы")
    val category: String,

    @Schema(description = "Уровень сложности", example = "Легко")
    val difficulty: String,

    @Schema(description = "Тип товара: COURSE для мастер-классов, null для физических товаров", example = "COURSE")
    val type: String? = null,

    @Schema(description = "Длительность курса в минутах", example = "90")
    val duration: Int? = null,

    @Schema(description = "ID видео в Kinescope для курсов", example = "kinescope_stub_12345")
    val videoUrl: String? = null,

    @Schema(description = "Список URL видео для галереи товара")
    val videoGalleryUrls: List<String>? = null,

    @Schema(description = "URL превью видео")
    val previewUrl: String? = null,

    @Schema(description = "Расширенное описание товара")
    val detailedDescription: String? = null,

    @Schema(description = "Рекомендации по уходу")
    val careInstructions: String? = null
)
