package com.example.webflux.domain.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.time.OffsetDateTime

// =====================================================
// ENUMS
// =====================================================
enum class UserRole {
    USER, BUYER, ADMIN
}

enum class GoodsType {
    PLANT,      // Растения
    TERRARIUM,  // Флорариумы
    COURSE      // Мастер-классы и курсы
}

// =====================================================
// TOKEN MODEL
// =====================================================
data class Token(
    val token: String,
    val userId: String,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime
)

// =====================================================
// USER MODEL
// =====================================================
data class User(
    val id: String?, // UUID
    val name: String,
    val email: String,
    val password: String,
    val roles: Set<UserRole> = DEFAULT_ROLES
) {
    private companion object {
        private val DEFAULT_ROLES: Set<UserRole> = setOf(UserRole.USER, UserRole.BUYER)
    }
}

// =====================================================
// CATEGORY MODEL
// =====================================================
data class Category(
    val id: String,
    val name: String,
    val type: GoodsType,
    val deletedAt: OffsetDateTime?
)

// =====================================================
// MEDIA MODELS (Sealed interface)
// =====================================================
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Image::class, name = "image"),
    JsonSubTypes.Type(value = Video::class, name = "video")
)
sealed interface Media {
    val url: String
    val order: Int
}

data class Image(
    override val url: String,
    override val order: Int = 0
) : Media

data class Video(
    override val url: String,
    override val order: Int = 0
) : Media

// =====================================================
// GOODS MODEL
// =====================================================
data class Goods(
    val id: String?,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val media: List<Media>,
    val categoryId: String,
    val difficulty: String,
    val duration: Int? = null,
    val videoUrl: String? = null,
    val detailedDescription: String? = null,
    val careInstructions: String? = null
)

// =====================================================
// CART MODELS
// =====================================================
/**
 * Внутренняя модель элемента корзины для хранения
 */
data class CartItem(
    val id: String? = null,      // UUID строки корзины (суррогатный PK; null для новой записи)
    val userId: String,          // UUID пользователя
    val goodsId: String?,        // UUID товара; NULL для кастомного флорариума
    val quantity: Int,           // Количество
    val addedAt: OffsetDateTime, // Время добавления
    // Поля кастомного заказа (заполнены, когда goodsId == null)
    val conversationId: String? = null,
    val imageUrl: String? = null,
    val customerComment: String? = null,
    val contact: String? = null
)

/**
 * Элемент корзины с полной информацией о товаре.
 * goods == null для кастомного флорариума (товара каталога нет) — тогда заполнены custom-поля.
 */
data class CartItemWithGoods(
    val id: String,
    val goods: Goods?,
    val category: Category?,
    val quantity: Int,
    val addedAt: OffsetDateTime,
    // Поля кастомного заказа (заполнены, когда goods == null)
    val conversationId: String? = null,
    val imageUrl: String? = null,
    val customerComment: String? = null,
    val contact: String? = null
)

/**
 * Сводка корзины с расчетом итогов
 */
data class CartSummary(
    val items: List<CartItemWithGoods>,
    val totalItems: Int,
    val totalPrice: BigDecimal
)

// =====================================================
// PURCHASE MODEL
// =====================================================
data class Purchase(
    val id: String?, // UUID - null для новых записей, БД генерирует автоматически
    val userId: String,
    val goodsId: String?,        // NULL для кастомного заказа флорариума
    val price: BigDecimal?,      // NULL у кастомного заказа до проставления цены админом
    val purchaseDate: OffsetDateTime,
    val quantity: Int = 1,
    // Поля кастомного заказа флорариума (заполнены, когда conversationId != null)
    val conversationId: String? = null,
    val imageUrl: String? = null,
    val customerComment: String? = null,
    val contact: String? = null,
    val status: String? = null   // NEW/IN_PROGRESS/DONE/CANCELLED; null для обычных покупок
)

// =====================================================
// REVIEW MODEL (Composite PK: goodsId + userId)
// =====================================================
data class Review(
    val goodsId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
