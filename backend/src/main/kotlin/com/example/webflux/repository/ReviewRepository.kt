package com.example.webflux.repository

import com.example.webflux.domain.model.Review
import com.example.webflux.entity.ReviewEntity
import com.example.webflux.mapper.ReviewMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

interface ReviewR2dbcRepository : CoroutineCrudRepository<ReviewEntity, String> {

    @Query("SELECT * FROM reviews WHERE goods_id = :goodsId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findByGoodsId(goodsId: String): Flow<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE goods_id IN (:ids) AND deleted_at IS NULL")
    fun findByGoodsIds(ids: List<String>): Flow<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findByUserId(userId: String): Flow<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE goods_id = :goodsId AND user_id = :userId AND deleted_at IS NULL")
    suspend fun findByGoodsIdAndUserId(goodsId: String, userId: String): ReviewEntity?

    @Query("""
        INSERT INTO reviews (goods_id, user_id, user_name, rating, comment, created_at, updated_at)
        VALUES (:goodsId, :userId, :userName, :rating, :comment, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (goods_id, user_id)
        DO UPDATE SET
            rating = EXCLUDED.rating,
            comment = EXCLUDED.comment,
            updated_at = CURRENT_TIMESTAMP
        RETURNING goods_id, user_id, user_name, rating, comment, created_at, updated_at, deleted_at
    """)
    suspend fun upsert(
        goodsId: String,
        userId: String,
        userName: String,
        rating: Int,
        comment: String
    ): ReviewEntity

    @Query("SELECT AVG(rating) FROM reviews WHERE goods_id = :goodsId AND deleted_at IS NULL")
    suspend fun getAverageRating(goodsId: String): Double?
}


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

    fun findByGoodsIds(ids: List<String>): Flow<Review> =
        reviewR2dbcRepository.findByGoodsIds(ids).map { ReviewMapper.toModel(it) }
}
