package com.example.webflux.controller

import com.example.webflux.controller.model.*
import com.example.webflux.security.SecurityUtils
import com.example.webflux.service.CartService
import com.example.webflux.service.PurchaseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * REST контроллер для управления корзиной покупок
 * Все endpoints требуют аутентификации и роль BUYER (проверяется через @PreAuthorize)
 */
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Корзина", description = "API управления корзиной покупок")
class CartController(
    private val cartService: CartService,
    private val purchaseService: PurchaseService
) {
    /**
     * Получить корзину текущего пользователя с расчётом итоговой суммы
     */
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Получить корзину", description = "Возвращает содержимое корзины с расчетом итоговой суммы")
    suspend fun getCart(): ResponseEntity<CartSummaryDto> =
        ResponseEntity.ok(cartService.getCartSummary(SecurityUtils.requireCurrentUserId()))

    /**
     * Добавить товар в корзину
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Добавить товар в корзину", description = "Добавляет товар в корзину или увеличивает количество если уже есть")
    suspend fun addItem(@org.springframework.validation.annotation.Validated @RequestBody request: AddToCartRequest): ResponseEntity<CartItemDto> =
        ResponseEntity.ok(cartService.addToCart(SecurityUtils.requireCurrentUserId(), request.goodsId, request.quantity))

    /**
     * Изменить количество товара в корзине
     */
    @PutMapping("/items/{goodsId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Изменить количество товара", description = "Обновляет количество единиц товара в корзине")
    suspend fun updateQuantity(
        @PathVariable goodsId: String,
        @org.springframework.validation.annotation.Validated @RequestBody request: UpdateQuantityRequest
    ): ResponseEntity<CartItemDto> {
        val item = cartService.updateQuantity(SecurityUtils.requireCurrentUserId(), goodsId, request.quantity)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(item)
    }

    /**
     * Удалить товар из корзины
     */
    @DeleteMapping("/items/{goodsId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Удалить товар из корзины", description = "Полностью удаляет товар из корзины")
    suspend fun removeItem(@PathVariable goodsId: String): ResponseEntity<Void> {
        cartService.removeFromCart(SecurityUtils.requireCurrentUserId(), goodsId)
        return ResponseEntity.noContent().build()
    }

    /**
     * Очистить корзину полностью
     */
    @DeleteMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Очистить корзину", description = "Удаляет все товары из корзины")
    suspend fun clearCart(): ResponseEntity<Void> {
        cartService.clearCart(SecurityUtils.requireCurrentUserId())
        return ResponseEntity.noContent().build()
    }

    /**
     * Синхронизировать локальную корзину с серверной при логине
     */
    @PostMapping("/merge")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(
        summary = "Синхронизировать локальную корзину",
        description = "Объединяет товары из localStorage с серверной корзиной при логине. При конфликтах суммируются количества"
    )
    suspend fun mergeCart(@org.springframework.validation.annotation.Validated @RequestBody request: MergeCartRequest): ResponseEntity<CartSummaryDto> =
        ResponseEntity.ok(cartService.mergeLocalCart(SecurityUtils.requireCurrentUserId(), request.items))

    /**
     * Оформить заказ
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Оформить заказ", description = "Оформляет покупку всех товаров из корзины. Создает Purchase записи для каждого товара.")
    suspend fun checkout(): ResponseEntity<CheckoutResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        val cart = cartService.getCartSummary(userId)

        if (cart.items.isEmpty())
            throw IllegalStateException("Корзина пуста")

        // Создаём Purchase для каждого товара
        cart.items.forEach { item ->
            repeat(item.quantity) {
                purchaseService.recordPurchase(userId, item.goods.id, item.goods.price)
            }
        }

        // Подготавливаем список купленных товаров
        val purchasedItems = cart.items.map { item ->
            PurchasedItem(
                goodsId = item.goods.id,
                goodsName = item.goods.name,
                quantity = item.quantity,
                price = item.goods.price
            )
        }

        val orderId = java.util.UUID.randomUUID().toString()
        val purchaseDate = java.time.Instant.now()

        // Очищаем корзину
        cartService.clearCart(userId)

        return ResponseEntity.ok(CheckoutResponse(
            orderId = orderId,
            totalPrice = cart.totalPrice,
            items = purchasedItems,
            purchaseDate = purchaseDate
        ))
    }
}
