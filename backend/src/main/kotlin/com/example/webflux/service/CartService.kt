package com.example.webflux.service

import com.example.webflux.controller.model.CartItemDto
import com.example.webflux.controller.model.CartSummaryDto
import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.controller.model.GoodsDto
import com.example.webflux.controller.model.CategoryDto
import com.example.webflux.controller.model.MediaDto
import com.example.webflux.controller.model.ImageDto
import com.example.webflux.controller.model.VideoDto
import com.example.webflux.domain.model.CartItem
import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.Media
import com.example.webflux.domain.model.Image
import com.example.webflux.domain.model.Video
import com.example.webflux.repository.CartRepository
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.repository.CategoryRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Сервис для управления корзиной покупок
 */
@Service
class CartService(
    private val cartRepository: CartRepository,
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * Получить сводку корзины с полной информацией о товарах и расчётом итогов
     */
    suspend fun getCartSummary(userId: Long): CartSummaryDto {
        val cartItems = cartRepository.findByUserId(userId)

        // Получаем полную информацию о товарах и фильтруем несуществующие
        val itemDtos = cartItems.mapNotNull { item ->
            val goods = goodsRepository.findById(item.goodsId.toLong())
            if (goods != null) {
                CartItemDto(item.id, goods.toDto(), item.quantity, item.addedAt)
            } else {
                // Товар удален из каталога - пропускаем
                null
            }
        }

        return CartSummaryDto(
            items = itemDtos,
            totalItems = itemDtos.sumOf { it.quantity },
            totalPrice = itemDtos.sumOf { (it.goods.price * it.quantity).toDouble() }
        )
    }

    /**
     * Добавить товар в корзину
     */
    suspend fun addToCart(userId: Long, goodsId: String, quantity: Int): CartItemDto {
        // Валидация: товар существует
        val goods = goodsRepository.findById(goodsId.toLong())
            ?: throw IllegalArgumentException("Goods not found: $goodsId")

        // Валидация: количество положительное
        require(quantity > 0) { "Quantity must be positive" }

        val cartItem = CartItem(
            id = UUID.randomUUID().toString(),
            userId = userId,
            goodsId = goodsId,
            quantity = quantity,
            addedAt = Instant.now()
        )

        val savedItem = cartRepository.addOrUpdateItem(cartItem)
        return CartItemDto(savedItem.id, goods.toDto(), savedItem.quantity, savedItem.addedAt)
    }

    /**
     * Изменить количество товара в корзине
     */
    suspend fun updateQuantity(userId: Long, goodsId: String, quantity: Int): CartItemDto? {
        require(quantity > 0) { "Use removeFromCart for deleting items" }

        val updated = cartRepository.updateQuantity(userId, goodsId, quantity)
            ?: return null

        val goods = goodsRepository.findById(goodsId.toLong())
            ?: throw IllegalStateException("Goods not found")

        return CartItemDto(updated.id, goods.toDto(), updated.quantity, updated.addedAt)
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeFromCart(userId: Long, goodsId: String): Boolean {
        return cartRepository.removeItem(userId, goodsId)
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

    // Extension функция для преобразования Media в MediaDto
    private fun Media.toDto(): MediaDto = when (this) {
        is Image -> ImageDto(url = url, order = order)
        is Video -> VideoDto(url = url, order = order)
    }

    // Extension функция для преобразования Goods в GoodsDto
    private suspend fun Goods.toDto() = GoodsDto(
        id = id,
        name = name,
        description = description,
        price = price,
        media = media.map { it.toDto() }.sortedBy { it.order },
        category = categoryRepository.findById(categoryId)?.let {
            CategoryDto(it.id, it.name, it.type)
        },
        difficulty = difficulty,
        duration = duration,
        videoUrl = videoUrl,
        previewUrl = previewUrl,
        detailedDescription = detailedDescription,
        careInstructions = careInstructions
    )
}
