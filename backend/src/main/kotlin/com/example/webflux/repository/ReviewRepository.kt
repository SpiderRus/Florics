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
        val entity = ReviewMapper.toEntity(review)
        val saved = reviewR2dbcRepository.save(entity)
        return ReviewMapper.toModel(saved)
    }

    fun findByGoodsId(goodsId: String): Flow<Review> =
        reviewR2dbcRepository.findByGoodsId(goodsId).map { ReviewMapper.toModel(it) }

    suspend fun findById(reviewId: String): Review? {
        val entity = reviewR2dbcRepository.findById(reviewId) ?: return null
        if (entity.deletedAt != null) return null
        return ReviewMapper.toModel(entity)
    }

    suspend fun deleteById(reviewId: String): Boolean {
        val exists = reviewR2dbcRepository.findById(reviewId)
        if (exists == null || exists.deletedAt != null) return false

        reviewR2dbcRepository.softDelete(reviewId)
        return true
    }
}
