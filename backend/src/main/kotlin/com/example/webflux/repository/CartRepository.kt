package com.example.webflux.repository

import com.example.webflux.domain.model.CartItem
import com.example.webflux.entity.CartItemEntity
import com.example.webflux.mapper.CartItemMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * R2DBC репозиторий для корзин покупок
 */

interface CartItemR2dbcRepository : CoroutineCrudRepository<CartItemEntity, String> {

    @Query("SELECT * FROM cart_items WHERE user_id = :userId")
    fun findByUserId(userId: String): Flow<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun findByUserIdAndGoodsId(userId: String, goodsId: String): CartItemEntity?

    @Query("DELETE FROM cart_items WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun deleteByUserIdAndGoodsId(userId: String, goodsId: String)

    @Query("DELETE FROM cart_items WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String): Int?

    @Query("""
        INSERT INTO cart_items (user_id, goods_id, quantity, added_at)
        VALUES (:userId, :goodsId, :quantity, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id, goods_id)
        DO UPDATE SET quantity = cart_items.quantity + :quantity
    """)
    suspend fun upsert(userId: String, goodsId: String, quantity: Int)
}


@Repository
class CartRepository(
    private val cartItemR2dbcRepository: CartItemR2dbcRepository,
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * Получить все товары корзины пользователя
     */
    suspend fun findByUserId(userId: String): List<CartItem> =
        cartItemR2dbcRepository.findByUserId(userId).map { CartItemMapper.toModel(it) }.toList()

    /**
     * Добавить товар в корзину или увеличить количество если уже существует
     */
    suspend fun addOrUpdateItem(item: CartItem): CartItem {
        val existing = cartItemR2dbcRepository.findByUserIdAndGoodsId(item.userId, item.goodsId)
        val entity = existing?.copy(quantity = existing.quantity + item.quantity) ?: CartItemMapper.toEntity(item)

        return CartItemMapper.toModel(cartItemR2dbcRepository.save(entity))
    }

    /**
     * Изменить количество товара в корзине
     * Если quantity <= 0, товар удаляется
     */
    suspend fun updateQuantity(userId: String, goodsId: String, quantity: Int): CartItem? {
        val item = cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId) ?: return null

        if (quantity <= 0)
            return cartItemR2dbcRepository.deleteByUserIdAndGoodsId(userId, goodsId).let { null }  // Дожидаемся выполнения

        return CartItemMapper.toModel(cartItemR2dbcRepository.save(item.copy(quantity = quantity)))
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeItem(userId: String, goodsId: String): Boolean {
        if (cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId) == null) return false

        return cartItemR2dbcRepository.deleteByUserIdAndGoodsId(userId, goodsId).let { true }  // Дожидаемся выполнения
    }

    /**
     * Очистить всю корзину пользователя
     */
    suspend fun clearCart(userId: String): Boolean = (cartItemR2dbcRepository.deleteByUserId(userId) ?: 0) != 0

    /**
     * Найти товар в корзине пользователя
     */
    suspend fun findByUserIdAndGoodsId(userId: String, goodsId: String): CartItem? =
        cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId)?.let { CartItemMapper.toModel(it) }
}
