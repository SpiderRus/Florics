package com.example.webflux.mapper

import com.example.webflux.domain.model.*
import com.example.webflux.entity.*
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

// =====================================================
// TOKEN MAPPER
// =====================================================
object TokenMapper {

    fun toEntity(token: Token): TokenEntity {
        return TokenEntity(
            token = null, // БД сгенерирует UUID автоматически
            userId = token.userId,
            createdAt = token.createdAt,
            expiresAt = token.expiresAt
        )
    }

    fun toModel(entity: TokenEntity): Token {
        return Token(
            token = entity.token ?: throw IllegalStateException("Token entity must have a token value"),
            userId = entity.userId,
            createdAt = entity.createdAt,
            expiresAt = entity.expiresAt
        )
    }
}

// =====================================================
// USER MAPPER
// =====================================================
object UserMapper {

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            // При создании нового пользователя передаём null, чтобы БД сгенерировала ID
            id = user.id,
            name = user.name,
            email = user.email,
            password = user.password,
            roles = user.roles.toList()
        )
    }

    fun toModel(entity: UserEntity): User {
        return User(
            id = entity.id ?: throw IllegalStateException("User entity must have an ID"),
            name = entity.name,
            email = entity.email,
            password = entity.password,
            roles = entity.roles.toSet()
        )
    }
}

// =====================================================
// CATEGORY MAPPER
// =====================================================
object CategoryMapper {
    fun toEntity(category: Category): CategoryEntity {
        return CategoryEntity(
            id = category.id,
            name = category.name,
            type = category.type.name,
            deletedAt = category.deletedAt
        )
    }

    fun toModel(entity: CategoryEntity): Category {
        return Category(
            id = entity.id ?: throw IllegalStateException("Category entity must have an ID"),
            name = entity.name,
            type = GoodsType.valueOf(entity.type),
            deletedAt = entity.deletedAt
        )
    }
}

// =====================================================
// MEDIA MAPPER
// =====================================================
object MediaMapper {

    fun toEntity(media: Media, goodsId: String): MediaEntity {
        return MediaEntity(
            id = null,
            goodsId = goodsId,
            type = when (media) {
                is Image -> "IMAGE"
                is Video -> "VIDEO"
            },
            url = media.url,
            displayOrder = media.order
        )
    }

    fun toModel(entity: MediaEntity): Media {
        return when (entity.type) {
            "IMAGE" -> Image(
                url = entity.url,
                order = entity.displayOrder
            )
            "VIDEO" -> Video(
                url = entity.url,
                order = entity.displayOrder
            )
            else -> throw IllegalArgumentException("Unknown media type: ${entity.type}")
        }
    }
}

// =====================================================
// GOODS MAPPER
// =====================================================
object GoodsMapper {

    fun toEntity(goods: Goods): GoodsEntity {
        return GoodsEntity(
            id = goods.id,
            name = goods.name,
            description = goods.description,
            price = goods.price,
            categoryId = goods.categoryId,
            difficulty = goods.difficulty,
            duration = goods.duration,
            videoUrl = goods.videoUrl,
            previewUrl = goods.previewUrl,
            detailedDescription = goods.detailedDescription,
            careInstructions = goods.careInstructions
        )
    }

    fun toModel(entity: GoodsEntity, media: List<Media>): Goods {
        return Goods(
            id = entity.id ?: throw IllegalStateException("Goods entity must have an ID"),
            name = entity.name,
            description = entity.description,
            price = entity.price,
            media = media,
            categoryId = entity.categoryId,
            difficulty = entity.difficulty,
            duration = entity.duration,
            videoUrl = entity.videoUrl,
            previewUrl = entity.previewUrl,
            detailedDescription = entity.detailedDescription,
            careInstructions = entity.careInstructions
        )
    }
}

// =====================================================
// CART ITEM MAPPER
// =====================================================
object CartItemMapper {
    fun toEntity(cartItem: CartItem): CartItemEntity {
        return CartItemEntity(
            userId = cartItem.userId,
            goodsId = cartItem.goodsId,
            quantity = cartItem.quantity,
            addedAt = cartItem.addedAt
        )
    }

    fun toModel(entity: CartItemEntity): CartItem {
        return CartItem(
            userId = entity.userId,
            goodsId = entity.goodsId,
            quantity = entity.quantity,
            addedAt = entity.addedAt
        )
    }
}

// =====================================================
// PURCHASE MAPPER
// =====================================================
object PurchaseMapper {

    fun toEntity(purchase: Purchase): PurchaseEntity {
        return PurchaseEntity(
            id = purchase.id,
            userId = purchase.userId,
            goodsId = purchase.goodsId,
            price = purchase.price,
            quantity = purchase.quantity,
            purchaseDate = purchase.purchaseDate
        )
    }

    fun toModel(entity: PurchaseEntity): Purchase {
        return Purchase(
            id = entity.id ?: throw IllegalStateException("Purchase entity must have an ID"),
            userId = entity.userId,
            goodsId = entity.goodsId,
            price = entity.price,
            quantity = entity.quantity,
            purchaseDate = entity.purchaseDate
        )
    }
}

// =====================================================
// REVIEW MAPPER
// =====================================================
object ReviewMapper {

    fun toEntity(review: Review): ReviewEntity {
        return ReviewEntity(
            goodsId = review.goodsId,
            userId = review.userId,
            userName = review.userName,
            rating = review.rating,
            comment = review.comment,
            createdAt = review.createdAt,
            updatedAt = review.updatedAt
        )
    }

    fun toModel(entity: ReviewEntity): Review {
        return Review(
            goodsId = entity.goodsId,
            userId = entity.userId,
            userName = entity.userName,
            rating = entity.rating,
            comment = entity.comment,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}

// =====================================================
// GOODS TYPE DOCUMENT MAPPER
// =====================================================
object GoodsTypeDocumentMapper {

    fun toEntity(model: GoodsTypeDocument): GoodsTypeDocumentEntity {
        return GoodsTypeDocumentEntity(
            documentId = model.documentId,
            goodsType = model.goodsType.name,
            createdAt = model.createdAt
        )
    }

    fun toModel(entity: GoodsTypeDocumentEntity): GoodsTypeDocument {
        return GoodsTypeDocument(
            documentId = entity.documentId,
            goodsType = GoodsType.valueOf(entity.goodsType),
            createdAt = entity.createdAt
        )
    }
}
