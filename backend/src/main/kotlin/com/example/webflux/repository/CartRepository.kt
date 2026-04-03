package com.example.webflux.repository

import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.domain.model.CartItem
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.mapper.CartItemMapper
import com.example.webflux.repository.r2dbc.CartItemR2dbcRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime

/**
 * R2DBC репозиторий для корзин покупок
 */
@Repository
class CartRepository(
    private val cartItemR2dbcRepository: CartItemR2dbcRepository,
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * Получить все товары корзины пользователя
     */
    suspend fun findByUserId(userId: String): List<CartItem> {
        return cartItemR2dbcRepository.findByUserId(userId)
            .toList()
            .map { CartItemMapper.toModel(it) }
    }

    /**
     * Добавить товар в корзину или увеличить количество если уже существует
     */
    suspend fun addOrUpdateItem(item: CartItem): CartItem {
        val existing = cartItemR2dbcRepository.findByUserIdAndGoodsId(item.userId, item.goodsId)

        return if (existing != null) {
            // Товар уже в корзине - суммируем количества
            val updated = existing.copy(quantity = existing.quantity + item.quantity)
            val saved = cartItemR2dbcRepository.save(updated)
            CartItemMapper.toModel(saved)
        } else {
            // Новый товар
            val entity = CartItemMapper.toEntity(item)
            val saved = cartItemR2dbcRepository.save(entity)
            CartItemMapper.toModel(saved)
        }
    }

    /**
     * Изменить количество товара в корзине
     * Если quantity <= 0, товар удаляется
     */
    suspend fun updateQuantity(userId: String, goodsId: String, quantity: Int): CartItem? {
        val item = cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId) ?: return null

        if (quantity <= 0) {
            cartItemR2dbcRepository.deleteByUserIdAndGoodsId(userId, goodsId)
            return null
        }

        val updated = item.copy(quantity = quantity)
        val saved = cartItemR2dbcRepository.save(updated)
        return CartItemMapper.toModel(saved)
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeItem(userId: String, goodsId: String): Boolean {
        if (cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId) == null) return false

        cartItemR2dbcRepository.deleteByUserIdAndGoodsId(userId, goodsId)
        return true
    }

    /**
     * Очистить всю корзину пользователя
     */
    suspend fun clearCart(userId: String): Boolean {
        if (cartItemR2dbcRepository.findByUserId(userId).toList().isEmpty()) return false

        cartItemR2dbcRepository.deleteByUserId(userId)
        return true
    }

    /**
     * Найти товар в корзине пользователя
     */
    suspend fun findByUserIdAndGoodsId(userId: String, goodsId: String): CartItem? {
        val entity = cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId) ?: return null
        return CartItemMapper.toModel(entity)
    }

    /**
     * Объединить локальные товары (из localStorage) с серверной корзиной
     * При конфликтах суммируются количества (кроме мастер-классов)
     */
    suspend fun mergeItems(userId: String, localItems: List<LocalCartItem>): List<CartItem> {
        localItems.forEach { localItem ->
            // Получить информацию о товаре и категории
            val goods = goodsRepository.findById(localItem.goodsId)
            val category = goods?.let { categoryRepository.findById(it.categoryId) }
            val isCourse = category?.type == GoodsType.COURSE

            val existing = cartItemR2dbcRepository.findByUserIdAndGoodsId(userId, localItem.goodsId)

            if (existing != null) {
                // Конфликт: товар есть и в серверной, и в локальной корзине
                // Для мастер-классов НЕ суммируем, оставляем quantity = 1
                if (!isCourse) {
                    // Для обычных товаров суммируем количества
                    val updated = existing.copy(quantity = existing.quantity + localItem.quantity)
                    cartItemR2dbcRepository.save(updated)
                }
            } else {
                // Добавляем новый товар из localStorage
                val finalQuantity = if (isCourse) 1 else localItem.quantity
                val newItem = CartItemMapper.toEntity(CartItem(
                    userId = userId,
                    goodsId = localItem.goodsId,
                    quantity = finalQuantity,
                    addedAt = OffsetDateTime.now()
                ))
                cartItemR2dbcRepository.save(newItem)
            }
        }

        return findByUserId(userId)
    }
}
