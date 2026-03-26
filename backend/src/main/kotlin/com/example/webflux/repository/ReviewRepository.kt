package com.example.webflux.repository

import com.example.webflux.model.Review
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class ReviewRepository {
    // reviewId -> Review
    private val reviewsById = ConcurrentHashMap<String, Review>()

    // plantId -> List<reviewId>
    private val reviewsByPlantId = ConcurrentHashMap<String, MutableList<String>>()

    suspend fun save(review: Review): Review {
        reviewsById[review.id] = review
        reviewsByPlantId.getOrPut(review.plantId) { mutableListOf() }.add(review.id)
        return review
    }

    suspend fun findByPlantId(plantId: String): List<Review> {
        val reviewIds = reviewsByPlantId[plantId] ?: return emptyList()
        return reviewIds.mapNotNull { reviewsById[it] }
    }

    suspend fun findById(reviewId: String): Review? {
        return reviewsById[reviewId]
    }

    suspend fun deleteById(reviewId: String): Boolean {
        val review = reviewsById.remove(reviewId) ?: return false
        reviewsByPlantId[review.plantId]?.remove(reviewId)
        return true
    }
}
