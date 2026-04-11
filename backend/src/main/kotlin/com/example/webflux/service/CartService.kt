package com.example.webflux.service

import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.domain.model.CartItem
import com.example.webflux.domain.model.CartItemWithGoods
import com.example.webflux.domain.model.CartSummary
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.repository.CartRepository
import com.example.webflux.repository.CategoryRepository
import com.example.webflux.repository.GoodsRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime

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
    suspend fun getCartSummary(userId: String): CartSummary {
        val cartItems = cartRepository.findByUserId(userId)

        // Получаем полную информацию о товарах и фильтруем несуществующие
        val itemsWithGoods = cartItems.mapNotNull { item ->
           goodsRepository.findById(item.goodsId)?.let { goods ->
                CartItemWithGoods(
                    id = generateId(item),
                    goods = goods,
                    category = categoryRepository.findById(goods.categoryId),
                    quantity = item.quantity,
                    addedAt = item.addedAt
                )
            }
        }

        return CartSummary(
            items = itemsWithGoods,
            totalItems = itemsWithGoods.sumOf { it.quantity },
            totalPrice = itemsWithGoods.sumOf { (it.goods.price * BigDecimal.valueOf(it.quantity.toLong())) }
        )
    }

    /**
     * Добавить товар в корзину
     */
    suspend fun addToCart(userId: String, goodsId: String, quantity: Int): CartItemWithGoods {
        // Валидация: товар существует
        val goods = goodsRepository.findById(goodsId) ?: throw IllegalArgumentException("Goods not found: $goodsId")

        // Получить категорию товара
        val category = categoryRepository.findById(goods.categoryId) ?: throw IllegalArgumentException("Category not found")

        // Для мастер-классов: проверить дубликаты и установить quantity = 1
        val finalQuantity = if (category.type == GoodsType.COURSE) {
                cartRepository.findByUserIdAndGoodsId(userId, goodsId)
                    ?.let { throw IllegalStateException("Course is already in cart") } ?: 1
            } else {
                require(quantity > 0) { "Quantity must be positive" }
                quantity
            }

        val cartItem = CartItem(
            userId = userId,
            goodsId = goodsId,
            quantity = finalQuantity,
            addedAt = OffsetDateTime.now()
        )

        val savedItem = cartRepository.addOrUpdateItem(cartItem)
        return CartItemWithGoods(
            id = generateId(savedItem),
            goods = goods,
            category = category,
            quantity = savedItem.quantity,
            addedAt = savedItem.addedAt
        )
    }

    /**
     * Изменить количество товара в корзине
     */
    suspend fun updateQuantity(userId: String, goodsId: String, quantity: Int): CartItemWithGoods? {
        require(quantity > 0) { "Use removeFromCart for deleting items" }

        val goods = goodsRepository.findById(goodsId) ?: throw IllegalStateException("Goods not found")

        val category = categoryRepository.findById(goods.categoryId) ?: throw IllegalStateException("Category not found")

        // Для мастер-классов запретить изменение количества
        if (category.type == GoodsType.COURSE && quantity != 1)
            throw IllegalArgumentException("Course quantity cannot be changed (must be 1)")

        val updated = cartRepository.updateQuantity(userId, goodsId, quantity) ?: return null

        return CartItemWithGoods(
            id = generateId(updated),
            goods = goods,
            category = category,
            quantity = updated.quantity,
            addedAt = updated.addedAt
        )
    }

    /**
     * Удалить товар из корзины
     */
    suspend fun removeFromCart(userId: String, goodsId: String): Boolean = cartRepository.removeItem(userId, goodsId)

    /**
     * Очистить всю корзину пользователя
     */
    suspend fun clearCart(userId: String): Boolean = cartRepository.clearCart(userId)

    /**
     * Синхронизировать локальную корзину (из localStorage) с серверной
     * При конфликтах суммируются количества
     */
    suspend fun mergeLocalCart(userId: String, localItems: List<LocalCartItem>): CartSummary {
        // Merge локальной корзины с серверной
        cartRepository.mergeItems(userId, localItems)

        // Вернуть обновленную корзину
        return getCartSummary(userId)
    }

    private companion object {
        fun generateId(cardItem: CartItem): String =
            "${cardItem.userId}-${cardItem.goodsId}" // Composite key as string
    }
}
