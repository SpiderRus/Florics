package com.example.webflux.repository

import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.model.CartItem
import kotlinx.coroutines.delay
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory репозиторий для корзин покупок
 * Хранит корзины в виде Map<userId, Map<plantId, CartItem>>
 */
@Repository
class CartRepository {
    // Двухуровневое хранилище: userId -> (plantId -> CartItem)
    private val storage = ConcurrentHashMap<Long, MutableMap<String, CartItem>>()

    /**
     * Получить все товары корзины пользователя
     */
    suspend fun findByUserId(userId: Long): List<CartItem> {
        delay(50) // Имитация БД
        return storage[userId]?.values?.toList() ?: emptyList()
    }

    /**
     * Добавить товар в корзину или увеличить количество если уже существует
     */
    suspend fun addOrUpdateItem(item: CartItem): CartItem {
        delay(50)
        val userCart = storage.getOrPut(item.userId) { mutableMapOf() }
        val existing = userCart[item.plantId]

        val updatedItem = existing?.// Товар уже в корзине - суммируем количества
                copy(quantity = existing.quantity + item.quantity)
            ?: // Новый товар
                item

        userCart[item.plantId] = updatedItem
        return updatedItem
    }

    /**
     * Изменить количество товара в корзине
     * Если quantity <= 0, товар удаляется
     */
    suspend fun updateQuantity(userId: Long, plantId: String, quantity: Int): CartItem? {
        delay(50)
        val userCart = storage[userId] ?: return null
        val item = userCart[plantId] ?: return null

        if (quantity <= 0) {
            userCart.remove(plantId)
            return null
        }

        val updated = item.copy(quantity = quantity)
        userCart[plantId] = updated
        return updated
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeItem(userId: Long, plantId: String): Boolean {
        delay(50)
        return storage[userId]?.remove(plantId) != null
    }

    /**
     * Очистить всю корзину пользователя
     */
    suspend fun clearCart(userId: Long): Boolean {
        delay(50)
        return storage.remove(userId) != null
    }

    /**
     * Объединить локальные товары (из localStorage) с серверной корзиной
     * При конфликтах суммируются количества
     */
    suspend fun mergeItems(userId: Long, localItems: List<LocalCartItem>): List<CartItem> {
        delay(100)
        val userCart = storage.getOrPut(userId) { mutableMapOf() }

        localItems.forEach { localItem ->
            val existing = userCart[localItem.plantId]

            if (existing != null) {
                // Конфликт: товар есть и в серверной, и в локальной корзине
                // Стратегия: суммировать количества
                userCart[localItem.plantId] = existing.copy(
                    quantity = existing.quantity + localItem.quantity
                )
            } else {
                // Добавляем новый товар из localStorage
                userCart[localItem.plantId] = CartItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    plantId = localItem.plantId,
                    quantity = localItem.quantity,
                    addedAt = Instant.now()
                )
            }
        }

        return userCart.values.toList()
    }
}
