package com.example.webflux.controller

import com.example.webflux.controller.model.CreateReviewRequest
import com.example.webflux.controller.model.GoodsRatingDto
import com.example.webflux.controller.model.GoodsRatingItemDto
import com.example.webflux.controller.model.ReviewDto
import com.example.webflux.controller.model.toReviewDto
import com.example.webflux.security.SecurityUtils
import com.example.webflux.service.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Отзывы", description = "API для работы с отзывами на товары")
class ReviewController(
    private val reviewService: ReviewService
) {
    @GetMapping("/{goodsId}")
    @Operation(summary = "Получить все отзывы на товар", description = "Возвращает список отзывов на товар, отсортированный по дате (новые первыми). Доступно всем пользователям.")
    fun getReviews(@PathVariable goodsId: String): Flow<ReviewDto> =
        reviewService.getReviewsByGoodsId(goodsId).map { it.toReviewDto() }

    @GetMapping("/rating/{goodsId}")
    @Operation(summary = "Получить средний рейтинг товара", description = "Возвращает средний рейтинг и количество отзывов. Доступно всем пользователям.")
    suspend fun getGoodsRating(@PathVariable goodsId: String): ResponseEntity<GoodsRatingDto> {
        val (averageRating, totalReviews) = reviewService.getGoodsRating(goodsId)

        return ResponseEntity.ok(GoodsRatingDto(averageRating, totalReviews))
    }

    @GetMapping("/ratings")
    @Operation(summary = "Рейтинги нескольких товаров", description = "Возвращает средние рейтинги для списка товаров одним запросом (устранение N+1 в каталоге). Доступно всем пользователям.")
    suspend fun getGoodsRatings(@RequestParam(required = false) ids: List<String>?): List<GoodsRatingItemDto> {
        if (ids.isNullOrEmpty()) return emptyList()
        return reviewService.getGoodsRatings(ids).map { (goodsId, pair) ->
            GoodsRatingItemDto(goodsId, pair.first, pair.second)
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Создать отзыв", description = "Создает новый отзыв. Доступно только авторизованным пользователям, которые купили товар.")
    suspend fun createReview(@org.springframework.validation.annotation.Validated @RequestBody request: CreateReviewRequest): ResponseEntity<ReviewDto> =
        ResponseEntity.ok(reviewService.createReview(
                userId = SecurityUtils.requireCurrentUserId(),
                goodsId = request.goodsId,
                rating = request.rating,
                comment = request.comment
            ).toReviewDto())

}
