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

    @Query("SELECT AVG(rating) FROM reviews WHERE goods_id = :goodsId AND deleted_at IS NULL")
    suspend fun getAverageRating(goodsId: String): Double?

    @Query("UPDATE reviews SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}
