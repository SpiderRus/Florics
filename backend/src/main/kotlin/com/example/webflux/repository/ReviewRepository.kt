package com.example.webflux.repository

import com.example.webflux.domain.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
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

    fun findByGoodsId(goodsId: String): Flow<Review> {
        val reviewIds = reviewsByGoodsId[goodsId]?.asFlow() ?: return emptyFlow()

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
