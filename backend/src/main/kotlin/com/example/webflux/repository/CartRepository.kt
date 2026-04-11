package com.example.webflux.repository

import com.example.webflux.controller.model.LocalCartItem
import com.example.webflux.domain.model.CartItem
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.entity.CartItemEntity
import com.example.webflux.mapper.CartItemMapper
import com.example.webflux.repository.r2dbc.CartItemR2dbcRepository
import com.example.webflux.util.associateBy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
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

            findByUserId(userId)
        } else emptyList()
    }
}
