package com.example.webflux.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

// =====================================================
// USER ENTITY
// =====================================================
@Table("users")
data class UserEntity(
    @Id
    val id: String? = null,

    @Column("name")
    val name: String,

    @Column("email")
    val email: String,

    @Column("password")
    val password: String,

    @Column("roles")
    val roles: List<String>,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null
)

// =====================================================
// CATEGORY ENTITY
// =====================================================
@Table("categories")
data class CategoryEntity(
    @Id
    val id: String? = null,

    @Column("name")
    val name: String,

    @Column("type")
    val type: String, // PLANT, TERRARIUM, COURSE

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null
)

// =====================================================
// GOODS ENTITY
// =====================================================
@Table("goods")
data class GoodsEntity(
    @Id
    val id: String? = null,

    @Column("name")
    val name: String,

    @Column("description")
    val description: String,

    @Column("price")
    val price: BigDecimal,

    @Column("category_id")
    val categoryId: String,

    @Column("difficulty")
    val difficulty: String,

    @Column("duration")
    val duration: Int? = null,

    @Column("video_url")
    val videoUrl: String? = null,

    @Column("preview_url")
    val previewUrl: String? = null,

    @Column("detailed_description")
    val detailedDescription: String? = null,

    @Column("care_instructions")
    val careInstructions: String? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null
)

// =====================================================
// MEDIA ENTITY
// =====================================================
@Table("media")
data class MediaEntity(
    @Id
    val id: String? = null,

    @Column("goods_id")
    val goodsId: String,

    @Column("type")
    val type: String, // IMAGE, VIDEO

    @Column("url")
    val url: String,

    @Column("display_order")
    val displayOrder: Int = 0,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null
)

// =====================================================
// CART ITEM ENTITY (Composite PK)
// =====================================================
@Table("cart_items")
data class CartItemEntity(
    @Column("user_id")
    val userId: String,

    @Column("goods_id")
    val goodsId: String,

    @Column("quantity")
    val quantity: Int,

    @Column("added_at")
    val addedAt: OffsetDateTime = OffsetDateTime.now()
)

// =====================================================
// PURCHASE ENTITY
// =====================================================
@Table("purchases")
data class PurchaseEntity(
    @Id
    val id: String? = null,

    @Column("user_id")
    val userId: String,

    @Column("goods_id")
    val goodsId: String,

    @Column("price")
    val price: BigDecimal,

    @Column("quantity")
    val quantity: Int,

    @Column("purchase_date")
    val purchaseDate: OffsetDateTime = OffsetDateTime.now(),

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null
)

// =====================================================
// REVIEW ENTITY
// =====================================================
@Table("reviews")
data class ReviewEntity(
    @Id
    val id: String? = null,

    @Column("goods_id")
    val goodsId: String,

    @Column("user_id")
    val userId: String,

    @Column("user_name")
    val userName: String,

    @Column("rating")
    val rating: Int,

    @Column("comment")
    val comment: String,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null
)

// =====================================================
// AI CONVERSATION ENTITY (Composite PK)
// =====================================================
@Table("ai_conversations")
data class AiConversationEntity(
    @Column("user_id")
    val userId: String,

    @Column("goods_id")
    val goodsId: String,

    @Column("conversation_id")
    val conversationId: String,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)
