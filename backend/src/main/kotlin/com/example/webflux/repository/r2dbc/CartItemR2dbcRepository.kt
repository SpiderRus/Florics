package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CartItemR2dbcRepository : CoroutineCrudRepository<CartItemEntity, String> {

    @Query("SELECT * FROM cart_items WHERE user_id = :userId")
    fun findByUserId(userId: String): Flow<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun findByUserIdAndGoodsId(userId: String, goodsId: String): CartItemEntity?

    @Query("DELETE FROM cart_items WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun deleteByUserIdAndGoodsId(userId: String, goodsId: String)

    @Query("DELETE FROM cart_items WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("""
        INSERT INTO cart_items (user_id, goods_id, quantity, added_at)
        VALUES (:userId, :goodsId, :quantity, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id, goods_id)
        DO UPDATE SET quantity = cart_items.quantity + :quantity
    """)
    suspend fun upsert(userId: String, goodsId: String, quantity: Int)
}
