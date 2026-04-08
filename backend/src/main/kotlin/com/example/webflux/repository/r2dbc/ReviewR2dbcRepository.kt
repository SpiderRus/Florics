package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReviewR2dbcRepository : CoroutineCrudRepository<ReviewEntity, String> {

    @Query("SELECT * FROM reviews WHERE goods_id = :goodsId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findByGoodsId(goodsId: String): Flow<ReviewEntity>

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
