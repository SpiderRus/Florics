package com.example.webflux.repository

import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.domain.model.CartItem
import com.example.webflux.domain.model.GoodsType
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory репозиторий для корзин покупок
 * Хранит корзины в виде Map<userId, Map<goodsId, CartItem>>
 */
@Repository
class CartRepository(
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {
    // Двухуровневое хранилище: userId -> (goodsId -> CartItem)
    private val storage = ConcurrentHashMap<Long, MutableMap<String, CartItem>>()

    /**
     * Получить все товары корзины пользователя
     */
    suspend fun findByUserId(userId: Long): List<CartItem> {
        return storage[userId]?.values?.toList() ?: emptyList()
    }

    /**
     * Добавить товар в корзину или увеличить количество если уже существует
     */
    suspend fun addOrUpdateItem(item: CartItem): CartItem {
        val userCart = storage.getOrPut(item.userId) { mutableMapOf() }
        val existing = userCart[item.goodsId]

        val updatedItem = existing?.// Товар уже в корзине - суммируем количества
                copy(quantity = existing.quantity + item.quantity)
            ?: // Новый товар
                item

        userCart[item.goodsId] = updatedItem
        return updatedItem
    }

    /**
     * Изменить количество товара в корзине
     * Если quantity <= 0, товар удаляется
     */
    suspend fun updateQuantity(userId: Long, goodsId: String, quantity: Int): CartItem? {
        val userCart = storage[userId] ?: return null
        val item = userCart[goodsId] ?: return null

        if (quantity <= 0) {
            userCart.remove(goodsId)
            return null
        }

        val updated = item.copy(quantity = quantity)
        userCart[goodsId] = updated
        return updated
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeItem(userId: Long, goodsId: String): Boolean {
        return storage[userId]?.remove(goodsId) != null
    }

    /**
     * Очистить всю корзину пользователя
     */
    suspend fun clearCart(userId: Long): Boolean {
        return storage.remove(userId) != null
    }

    /**
     * Найти товар в корзине пользователя
     */
    suspend fun findByUserIdAndGoodsId(userId: Long, goodsId: String): CartItem? {
        return storage[userId]?.get(goodsId)
    }

    /**
     * Объединить локальные товары (из localStorage) с серверной корзиной
     * При конфликтах суммируются количества (кроме мастер-классов)
     */
    suspend fun mergeItems(userId: Long, localItems: List<LocalCartItem>): List<CartItem> {
        val userCart = storage.getOrPut(userId) { mutableMapOf() }

        localItems.forEach { localItem ->
            // Получить информацию о товаре и категории
            val goods = goodsRepository.findById(localItem.goodsId.toLong())
            val category = goods?.let { categoryRepository.findById(it.categoryId) }
            val isCourse = category?.type == GoodsType.COURSE

            val existing = userCart[localItem.goodsId]

            if (existing != null) {
                // Конфликт: товар есть и в серверной, и в локальной корзине
                // Для мастер-классов НЕ суммируем, оставляем quantity = 1
                // Ничего не делаем, курс уже в корзине с quantity = 1
                if (!isCourse) {
                    // Для обычных товаров суммируем количества
                    userCart[localItem.goodsId] = existing.copy(quantity = existing.quantity + localItem.quantity)
                }
            } else {
                // Добавляем новый товар из localStorage
                val finalQuantity = if (isCourse) 1 else localItem.quantity
                userCart[localItem.goodsId] = CartItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    goodsId = localItem.goodsId,
                    quantity = finalQuantity,
                    addedAt = Instant.now()
                )
            }
        }

        return userCart.values.toList()
    }
}
