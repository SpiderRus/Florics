package com.example.webflux.service

import com.example.webflux.domain.model.Review
import com.example.webflux.repository.ReviewRepository
import com.example.webflux.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val purchaseService: PurchaseService,
    private val userRepository: UserRepository
) {
    suspend fun createReview(userId: String, goodsId: String, rating: Int, comment: String): Review {
        // Проверка: купил ли пользователь этот товар
        if (!purchaseService.hasPurchased(userId, goodsId))
            throw IllegalStateException("Вы можете оставить отзыв только на купленные товары")

        // Валидация рейтинга
        if (rating !in 1..5)
            throw IllegalArgumentException("Рейтинг должен быть от 1 до 5")

        // Получаем имя пользователя
        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("Пользователь не найден")

        val review = Review(
            id = null, // БД сгенерирует ID автоматически
            goodsId = goodsId,
            userId = userId,
            userName = user.name,
            rating = rating,
            comment = comment.trim(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        return reviewRepository.save(review)
    }

    fun getReviewsByGoodsId(goodsId: String): Flow<Review> = flow {
        reviewRepository.findByGoodsId(goodsId).toList().sortedByDescending { it.createdAt }.forEach { emit(it) }
    }

    suspend fun getGoodsRating(goodsId: String): Pair<Double, Int> {
        val reviews = reviewRepository.findByGoodsId(goodsId).map { it.rating }.toList()

        return if (reviews.isEmpty()) Pair(0.0, 0) else Pair(reviews.average(), reviews.size)
    }

    suspend fun deleteReview(userId: String, reviewId: String): Boolean {
        val review = reviewRepository.findById(reviewId) ?: throw IllegalArgumentException("Отзыв не найден")

        // Проверка: только автор может удалить отзыв
        if (review.userId != userId)
            throw IllegalStateException("Вы можете удалить только свой отзыв")

        return reviewRepository.deleteById(reviewId)
    }
}
