package com.example.webflux.domain.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.r2dbc.postgresql.message.frontend.Bind
import java.math.BigDecimal
import java.time.OffsetDateTime

// =====================================================
// ENUMS
// =====================================================
enum class GoodsType {
    PLANT,      // Растения
    TERRARIUM,  // Флорариумы
    COURSE      // Мастер-классы и курсы
}

// =====================================================
// USER MODEL
// =====================================================
data class User(
    val id: String?, // UUID
    val name: String,
    val email: String,
    val password: String,
    val roles: Set<String> = setOf("USER")
)

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
    val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val media: List<Media>,
    val categoryId: String,
    val difficulty: String,
    val duration: Int? = null,
    val videoUrl: String? = null,
    val previewUrl: String? = null,
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
    val userId: String,          // UUID пользователя
    val goodsId: String,         // UUID товара
    val quantity: Int,           // Количество
    val addedAt: OffsetDateTime  // Время добавления
)

/**
 * Элемент корзины с полной информацией о товаре
 */
data class CartItemWithGoods(
    val id: String,
    val goods: Goods,
    val category: Category?,
    val quantity: Int,
    val addedAt: OffsetDateTime
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
    val goodsId: String,
    val price: BigDecimal,
    val purchaseDate: OffsetDateTime,
    val quantity: Int = 1
)

// =====================================================
// REVIEW MODEL
// =====================================================
data class Review(
    val id: String?, // UUID - null для новых записей, БД генерирует автоматически
    val goodsId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

// =====================================================
// GOODS TYPE DOCUMENT MODEL
// =====================================================
/**
 * Связь между типом товара и документом AI Agent
 *
 * Представляет документ, загруженный в AI Agent и ассоциированный
 * с конкретным типом товаров (PLANT, TERRARIUM, COURSE). Документ
 * становится доступен для RAG запросов при чате о товарах этого типа.
 */
data class GoodsTypeDocument(
    val documentId: String,        // UUID из AI Agent DocumentResponse.id
    val goodsType: GoodsType,      // PLANT, TERRARIUM или COURSE
    val createdAt: OffsetDateTime  // Когда создана связь
)
