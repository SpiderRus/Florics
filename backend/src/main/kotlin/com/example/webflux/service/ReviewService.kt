package com.example.webflux.service

import com.example.webflux.repository.model.Review
import com.example.webflux.repository.ReviewRepository
import com.example.webflux.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val purchaseService: PurchaseService,
    private val userRepository: UserRepository
) {
    suspend fun createReview(userId: Long, plantId: String, rating: Int, comment: String): Review {
        // Проверка: купил ли пользователь этот товар
        if (!purchaseService.hasPurchased(userId, plantId))
            throw IllegalStateException("Вы можете оставить отзыв только на купленные товары")

        // Валидация рейтинга
        if (rating !in 1..5)
            throw IllegalArgumentException("Рейтинг должен быть от 1 до 5")

        // Получаем имя пользователя
        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("Пользователь не найден")

        val review = Review(
            id = UUID.randomUUID().toString(),
            plantId = plantId,
            userId = userId,
            userName = user.name,
            rating = rating,
            comment = comment.trim(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        return reviewRepository.save(review)
    }

    suspend fun getReviewsByPlantId(plantId: String): List<Review> {
        return reviewRepository.findByPlantId(plantId)
            .sortedByDescending { it.createdAt }
    }

    suspend fun getPlantRating(plantId: String): Pair<Double, Int> {
        val reviews = reviewRepository.findByPlantId(plantId)
        if (reviews.isEmpty())
            return Pair(0.0, 0)

        val averageRating = reviews.map { it.rating }.average()
        return Pair(averageRating, reviews.size)
    }

    suspend fun deleteReview(userId: Long, reviewId: String): Boolean {
        val review = reviewRepository.findById(reviewId)
            ?: throw IllegalArgumentException("Отзыв не найден")

        // Проверка: только автор может удалить отзыв
        if (review.userId != userId)
            throw IllegalStateException("Вы можете удалить только свой отзыв")

        return reviewRepository.deleteById(reviewId)
    }
}
