package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseR2dbcRepository : CoroutineCrudRepository<PurchaseEntity, String> {

    @Query("SELECT * FROM purchases WHERE user_id = :userId AND deleted_at IS NULL ORDER BY purchase_date DESC")
    fun findByUserId(userId: String): Flow<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE goods_id = :goodsId AND deleted_at IS NULL ORDER BY purchase_date DESC")
    fun findByGoodsId(goodsId: String): Flow<PurchaseEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM purchases WHERE user_id = :userId AND goods_id = :goodsId AND deleted_at IS NULL)")
    suspend fun existsByUserIdAndGoodsId(userId: String, goodsId: String): Boolean

    @Query("UPDATE purchases SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}
