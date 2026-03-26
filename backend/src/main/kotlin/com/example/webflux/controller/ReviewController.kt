package com.example.webflux.controller

import com.example.webflux.controller.model.CreateReviewRequest
import com.example.webflux.controller.model.PlantRatingDto
import com.example.webflux.controller.model.ReviewDto
import com.example.webflux.security.SecurityUtils
import com.example.webflux.service.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Отзывы", description = "API для работы с отзывами на товары")
class ReviewController(
    private val reviewService: ReviewService
) {
    @GetMapping("/{plantId}")
    @Operation(summary = "Получить все отзывы на товар", description = "Возвращает список отзывов на товар, отсортированный по дате (новые первыми). Доступно всем пользователям.")
    suspend fun getReviews(@PathVariable plantId: String): Flow<ReviewDto> {
        return reviewService.getReviewsByPlantId(plantId)
            .map { ReviewDto.fromReview(it) }
            .asFlow()
    }

    @GetMapping("/rating/{plantId}")
    @Operation(summary = "Получить средний рейтинг товара", description = "Возвращает средний рейтинг и количество отзывов. Доступно всем пользователям.")
    suspend fun getPlantRating(@PathVariable plantId: String): ResponseEntity<PlantRatingDto> {
        val (averageRating, totalReviews) = reviewService.getPlantRating(plantId)
        return ResponseEntity.ok(PlantRatingDto(averageRating, totalReviews))
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Создать отзыв", description = "Создает новый отзыв. Доступно только авторизованным пользователям, которые купили товар.")
    suspend fun createReview(@RequestBody request: CreateReviewRequest): ResponseEntity<ReviewDto> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("Пользователь не авторизован")

        val review = reviewService.createReview(
            userId = userId,
            plantId = request.plantId,
            rating = request.rating,
            comment = request.comment
        )

        return ResponseEntity.ok(ReviewDto.fromReview(review))
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Удалить отзыв", description = "Удаляет отзыв. Только автор может удалить свой отзыв.")
    suspend fun deleteReview(@PathVariable reviewId: String): ResponseEntity<Void> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("Пользователь не авторизован")

        reviewService.deleteReview(userId, reviewId)

        return ResponseEntity.noContent().build()
    }
}
