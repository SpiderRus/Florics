package com.example.webflux.repository

import com.example.webflux.domain.model.Review
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class ReviewRepository {
    // reviewId -> Review
    private val reviewsById = ConcurrentHashMap<String, Review>()

    // goodsId -> List<reviewId>
    private val reviewsByGoodsId = ConcurrentHashMap<String, MutableList<String>>()

    suspend fun save(review: Review): Review {
        reviewsById[review.id] = review
        reviewsByGoodsId.getOrPut(review.goodsId) { mutableListOf() }.add(review.id)
        return review
    }

    suspend fun findByGoodsId(goodsId: String): List<Review> {
        val reviewIds = reviewsByGoodsId[goodsId] ?: return emptyList()
        return reviewIds.mapNotNull { reviewsById[it] }
    }

    suspend fun findById(reviewId: String): Review? {
        return reviewsById[reviewId]
    }

    suspend fun deleteById(reviewId: String): Boolean {
        val review = reviewsById.remove(reviewId) ?: return false
        reviewsByGoodsId[review.goodsId]?.remove(reviewId)
        return true
    }
}
