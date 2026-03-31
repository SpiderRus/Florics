package com.example.webflux.controller.model

import com.example.webflux.domain.model.Review
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Запрос на создание отзыва")
data class CreateReviewRequest(
    @Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @Schema(description = "Рейтинг от 1 до 5", example = "5")
    val rating: Int,

    @Schema(description = "Текст отзыва", example = "Отличное растение! Быстро прижилось и радует меня каждый день.")
    val comment: String
)

@Schema(description = "Отзыв о товаре")
data class ReviewDto(
    @Schema(description = "ID отзыва")
    val id: String,

    @Schema(description = "ID товара")
    val goodsId: String,

    @Schema(description = "Имя автора")
    val userName: String,

    @Schema(description = "Рейтинг от 1 до 5")
    val rating: Int,

    @Schema(description = "Текст отзыва")
    val comment: String,

    @Schema(description = "Дата создания")
    val createdAt: Instant,

    @Schema(description = "Дата обновления")
    val updatedAt: Instant
)

// Extension функция для преобразования domain entity в DTO
fun Review.toDto() = ReviewDto(
    id = id,
    goodsId = goodsId,
    userName = userName,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@Schema(description = "Средний рейтинг товара")
data class GoodsRatingDto(
    @Schema(description = "Средний рейтинг (0.0 если нет отзывов)", example = "4.5")
    val averageRating: Double,

    @Schema(description = "Количество отзывов", example = "12")
    val totalReviews: Int
)
