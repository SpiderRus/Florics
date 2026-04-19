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

    @Query("""
        UPDATE cart_items
        SET quantity = :quantity
        WHERE user_id = :userId AND goods_id = :goodsId
    """)
    suspend fun updateQuantityByUserAndGoods(userId: String, goodsId: String, quantity: Int): Int
}


@Repository
class CartRepository(
    private val cartItemR2dbcRepository: CartItemR2dbcRepository
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
        // Используем upsert для корректной работы с составным ключом
        cartItemR2dbcRepository.upsert(item.userId, item.goodsId, item.quantity)

        // Загружаем обновлённую запись
        return cartItemR2dbcRepository.findByUserIdAndGoodsId(item.userId, item.goodsId)
            ?.let { CartItemMapper.toModel(it) }
            ?: throw IllegalStateException("Failed to add or update cart item")
    }

    /**
     * Изменить количество товара в корзине
     * Если quantity <= 0, товар удаляется
     */
    suspend fun updateQuantity(userId: String, goodsId: String, quantity: Int): CartItem? {
        val item = cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId) ?: return null

        if (quantity <= 0)
            return cartItemR2dbcRepository.deleteByUserIdAndGoodsId(userId, goodsId).let { null }  // Дожидаемся выполнения

        // Используем кастомный UPDATE вместо save() для entity с составным ключом
        val updatedRows = cartItemR2dbcRepository.updateQuantityByUserAndGoods(userId, goodsId, quantity)

        if (updatedRows == 0) return null

        // Загружаем обновлённую запись
        return cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId)?.let { CartItemMapper.toModel(it) }
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
