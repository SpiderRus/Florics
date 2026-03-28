package com.example.webflux.service

import com.example.webflux.controller.model.CartItemDto
import com.example.webflux.controller.model.CartSummaryDto
import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.repository.model.CartItem
import com.example.webflux.repository.CartRepository
import com.example.webflux.repository.PlantRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Сервис для управления корзиной покупок
 */
@Service
class CartService(
    private val cartRepository: CartRepository,
    private val plantRepository: PlantRepository
) {
    /**
     * Получить сводку корзины с полной информацией о растениях и расчётом итогов
     */
    suspend fun getCartSummary(userId: Long): CartSummaryDto {
        val cartItems = cartRepository.findByUserId(userId)

        // Получаем полную информацию о растениях и фильтруем несуществующие
        val itemDtos = cartItems.mapNotNull { item ->
            val plant = plantRepository.findById(item.plantId.toLong())
            if (plant != null) {
                CartItemDto(item.id, plant, item.quantity, item.addedAt)
            } else {
                // Растение удалено из каталога - пропускаем
                null
            }
        }

        return CartSummaryDto(
            items = itemDtos,
            totalItems = itemDtos.sumOf { it.quantity },
            totalPrice = itemDtos.sumOf { it.plant.price * it.quantity }
        )
    }

    /**
     * Добавить товар в корзину
     */
    suspend fun addToCart(userId: Long, plantId: String, quantity: Int): CartItemDto {
        // Валидация: растение существует
        val plant = plantRepository.findById(plantId.toLong())
            ?: throw IllegalArgumentException("Plant not found: $plantId")

        // Валидация: количество положительное
        require(quantity > 0) { "Quantity must be positive" }

        val cartItem = CartItem(
            id = UUID.randomUUID().toString(),
            userId = userId,
            plantId = plantId,
            quantity = quantity,
            addedAt = Instant.now()
        )

        val savedItem = cartRepository.addOrUpdateItem(cartItem)
        return CartItemDto(savedItem.id, plant, savedItem.quantity, savedItem.addedAt)
    }

    /**
     * Изменить количество товара в корзине
     */
    suspend fun updateQuantity(userId: Long, plantId: String, quantity: Int): CartItemDto? {
        require(quantity > 0) { "Use removeFromCart for deleting items" }

        val updated = cartRepository.updateQuantity(userId, plantId, quantity)
            ?: return null

        val plant = plantRepository.findById(plantId.toLong())
            ?: throw IllegalStateException("Plant not found")

        return CartItemDto(updated.id, plant, updated.quantity, updated.addedAt)
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeFromCart(userId: Long, plantId: String): Boolean {
        return cartRepository.removeItem(userId, plantId)
    }

    /**
     * Очистить всю корзину пользователя
     */
    suspend fun clearCart(userId: Long): Boolean {
        return cartRepository.clearCart(userId)
    }

    /**
     * Синхронизировать локальную корзину (из localStorage) с серверной
     * При конфликтах суммируются количества
     */
    suspend fun mergeLocalCart(userId: Long, localItems: List<LocalCartItem>): CartSummaryDto {
        // Merge локальной корзины с серверной
        cartRepository.mergeItems(userId, localItems)
        // Вернуть обновленную корзину
        return getCartSummary(userId)
    }
}
