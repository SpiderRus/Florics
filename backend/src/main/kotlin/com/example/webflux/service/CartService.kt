package com.example.webflux.service

import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.domain.model.CartItem
import com.example.webflux.domain.model.CartItemWithGoods
import com.example.webflux.domain.model.CartSummary
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.entity.CartItemEntity
import com.example.webflux.mapper.CartItemMapper
import com.example.webflux.repository.AiConversationRepository
import com.example.webflux.repository.CartItemR2dbcRepository
import com.example.webflux.repository.CartRepository
import com.example.webflux.repository.CategoryRepository
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.util.associateBy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
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
    private val categoryRepository: CategoryRepository,
    private val cartItemR2dbcRepository: CartItemR2dbcRepository,
    private val aiConversationRepository: AiConversationRepository
) {
    /**
     * Получить сводку корзины с полной информацией о товарах и расчётом итогов
     */
    suspend fun getCartSummary(userId: String): CartSummary {
        val cartItems = cartRepository.findByUserId(userId)

        // Получаем полную информацию о товарах и фильтруем несуществующие
        val itemsWithGoods = cartItems.mapNotNull { item ->
            if (item.goodsId == null) {
                // Кастомный флорариум — товара каталога нет, цену проставит админ
                CartItemWithGoods(
                    id = item.id!!,
                    goods = null,
                    category = null,
                    quantity = item.quantity,
                    addedAt = item.addedAt,
                    conversationId = item.conversationId,
                    imageUrl = item.imageUrl,
                    customerComment = item.customerComment,
                    contact = item.contact
                )
            } else {
                goodsRepository.findById(item.goodsId)?.let { goods ->
                    CartItemWithGoods(
                        id = item.id!!,
                        goods = goods,
                        category = categoryRepository.findById(goods.categoryId),
                        quantity = item.quantity,
                        addedAt = item.addedAt
                    )
                }
            }
        }

        return CartSummary(
            items = itemsWithGoods,
            totalItems = itemsWithGoods.sumOf { it.quantity },
            // Кастомные элементы (goods == null) пока без цены — в сумму не входят
            totalPrice = itemsWithGoods.sumOf { item ->
                item.goods?.let { it.price * BigDecimal.valueOf(item.quantity.toLong()) } ?: BigDecimal.ZERO
            }
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
            id = savedItem.id!!,
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

        // Проверяем что товар уже есть в корзине
        val existingCartItem = cartRepository.findByUserIdAndGoodsId(userId, goodsId) ?: return null

        val goods = goodsRepository.findById(goodsId) ?: throw IllegalStateException("Goods not found")

        val category = categoryRepository.findById(goods.categoryId) ?: throw IllegalStateException("Category not found")

        // Для мастер-классов запретить изменение количества
        if (category.type == GoodsType.COURSE && quantity != 1)
            throw IllegalArgumentException("Course quantity cannot be changed (must be 1)")

        // Вычисляем дельту для добавления
        val quantityDelta = quantity - existingCartItem.quantity

        // Используем addOrUpdateItem с дельтой (upsert увеличит на delta)
        val updated = cartRepository.addOrUpdateItem(
            CartItem(
                userId = userId,
                goodsId = goodsId,
                quantity = quantityDelta,  // Передаём дельту, не абсолютное значение
                addedAt = existingCartItem.addedAt
            )
        )

        return CartItemWithGoods(
            id = updated.id!!,
            goods = goods,
            category = category,
            quantity = updated.quantity,
            addedAt = updated.addedAt
        )
    }

    /**
     * Добавить кастомный флорариум в корзину (по результатам чата с дизайнером).
     * Всегда новая строка (quantity = 1), цена будет проставлена админом при обработке заказа.
     */
    suspend fun addCustomFlorarium(
        userId: String,
        conversationId: String,
        imageUrl: String,
        comment: String?,
        contact: String?
    ): CartItemWithGoods {
        // Валидация: разговор существует и принадлежит пользователю
        val ownerId = aiConversationRepository.findByConversationId(conversationId)
            ?: throw IllegalArgumentException("Conversation not found: $conversationId")
        require(ownerId == userId) { "Conversation does not belong to user" }
        require(imageUrl.isNotBlank()) { "Image URL is required" }

        val saved = cartRepository.insertCustomItem(
            CartItem(
                userId = userId,
                goodsId = null,
                quantity = 1,
                addedAt = OffsetDateTime.now(),
                conversationId = conversationId,
                imageUrl = imageUrl,
                customerComment = comment?.takeIf { it.isNotBlank() },
                contact = contact?.takeIf { it.isNotBlank() }
            )
        )

        return CartItemWithGoods(
            id = saved.id!!,
            goods = null,
            category = null,
            quantity = saved.quantity,
            addedAt = saved.addedAt,
            conversationId = saved.conversationId,
            imageUrl = saved.imageUrl,
            customerComment = saved.customerComment,
            contact = saved.contact
        )
    }

    /**
     * Удалить кастомный элемент корзины по id (с проверкой владельца).
     */
    suspend fun removeCustomItem(userId: String, id: String): Boolean {
        val item = cartRepository.findById(id) ?: return false
        require(item.userId == userId) { "Cart item does not belong to user" }
        cartRepository.deleteById(id)
        return true
    }

    /**
     * Объединить локальные товары (из localStorage) с серверной корзиной
     * При конфликтах суммируются количества (кроме мастер-классов)
     */
    suspend fun mergeItems(userId: String, localItems: List<LocalCartItem>): List<CartItem> = coroutineScope {
        if (localItems.isNotEmpty()) {
            val existingMap = async { cartItemR2dbcRepository.findByUserId(userId).associateBy { it.goodsId } }
            val goodsMap = async { goodsRepository.findByIdsIn(localItems.map { it.goodsId }).associateBy { it.id } }
            val updatedItems = ArrayList<CartItemEntity>()

            localItems.forEach { localItem ->
                // Получить информацию о товаре и категории
                val goods = goodsMap.await()[localItem.goodsId]
                val category = goods?.let { categoryRepository.findById(it.categoryId) }
                val isCourse = category?.type == GoodsType.COURSE

                val existing = existingMap.await()[localItem.goodsId]

                if (existing != null) {
                    // Конфликт: товар есть и в серверной, и в локальной корзине
                    // Для мастер-классов НЕ суммируем, оставляем quantity = 1
                    if (!isCourse) {
                        // Для обычных товаров суммируем количества
                        updatedItems.add(existing.copy(quantity = existing.quantity + localItem.quantity))
                    }
                } else {
                    // Добавляем новый товар из localStorage
                    val finalQuantity = if (isCourse) 1 else localItem.quantity
                    val newItem = CartItemMapper.toEntity(
                        CartItem(
                            userId = userId,
                            goodsId = localItem.goodsId,
                            quantity = finalQuantity,
                            addedAt = OffsetDateTime.now()
                        )
                    )
                    updatedItems.add(newItem)
                }
            }

            if (updatedItems.isNotEmpty())
                cartItemR2dbcRepository.saveAll(updatedItems).collect()

            cartRepository.findByUserId(userId)
        } else emptyList()
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
        mergeItems(userId, localItems)

        // Вернуть обновленную корзину
        return getCartSummary(userId)
    }
}
