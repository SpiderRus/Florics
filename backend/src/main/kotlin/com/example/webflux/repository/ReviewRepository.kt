package com.example.webflux.repository

import com.example.webflux.domain.model.Review
import com.example.webflux.mapper.ReviewMapper
import com.example.webflux.repository.r2dbc.ReviewR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Repository

@Repository
class ReviewRepository(
    private val reviewR2dbcRepository: ReviewR2dbcRepository
) {

    suspend fun save(review: Review): Review {
        val saved = reviewR2dbcRepository.upsert(
            goodsId = review.goodsId,
            userId = review.userId,
            userName = review.userName,
            rating = review.rating,
            comment = review.comment
        )

        return ReviewMapper.toModel(saved)
    }

    fun findByGoodsId(goodsId: String): Flow<Review> =
        reviewR2dbcRepository.findByGoodsId(goodsId).map { ReviewMapper.toModel(it) }

    suspend fun findByGoodsIdAndUserId(goodsId: String, userId: String): Review? {
        val entity = reviewR2dbcRepository.findByGoodsIdAndUserId(goodsId, userId) ?: return null
        return ReviewMapper.toModel(entity)
    }
}
