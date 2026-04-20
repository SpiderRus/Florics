package com.example.webflux.controller.model

import com.example.webflux.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.OffsetDateTime

// ============================================================================
// Authentication & User DTOs
// ============================================================================

@Schema(description = "Данные пользователя (без пароля)")
data class UserDto(
    @field:Schema(description = "ID пользователя")
    val id: String,

    @field:Schema(description = "Имя пользователя")
    val name: String,

    @field:Schema(description = "Email пользователя")
    val email: String,

    @field:Schema(description = "Может ли пользователь совершать покупки (имеет роль BUYER)")
    val canPurchase: Boolean,

    @field:Schema(description = "Является ли пользователь администратором (только для UI подсказок)")
    val isAdmin: Boolean? = null
)

// ============================================================================
// Goods & Category DTOs
// ============================================================================

@Schema(description = "Модель категории товаров для API")
data class CategoryDto(
    @field:Schema(description = "Уникальный идентификатор категории", example = "1")
    val id: String,

    @field:Schema(description = "Название категории", example = "Лианы")
    val name: String,

    @field:Schema(description = "Тип товаров в категории", example = "PHYSICAL")
    val type: GoodsType
)

@Schema(description = "Медиа элемент (изображение или видео)")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ImageDto::class, name = "image"),
    JsonSubTypes.Type(value = VideoDto::class, name = "video")
)
sealed interface MediaDto {
    val type: String
    val url: String
    val order: Int
}

@Schema(description = "Изображение")
data class ImageDto(
    override val type: String = "image",
    @field:Schema(description = "URL изображения")
    override val url: String,
    @field:Schema(description = "Порядок отображения")
    override val order: Int = 0
) : MediaDto

@Schema(description = "Видео")
data class VideoDto(
    override val type: String = "video",
    @field:Schema(description = "URL видео")
    override val url: String,
    @field:Schema(description = "Порядок отображения")
    override val order: Int = 0
) : MediaDto

@Schema(description = "Модель товара для API")
data class GoodsDto(
    @field:Schema(description = "Уникальный идентификатор", example = "1")
    val id: String,

    @field:Schema(description = "Название", example = "Монстера деликатесная")
    val name: String,

    @field:Schema(
        description = "Краткое описание",
        example = "Популярная тропическая лиана с крупными резными листьями"
    )
    val description: String,

    @field:Schema(description = "Цена в рублях", example = "1500.0")
    val price: Double,

    @field:Schema(description = "Список медиа-элементов (изображения и видео)")
    val media: List<MediaDto>,

    @field:Schema(description = "Информация о категории")
    val category: CategoryDto?,

    @field:Schema(description = "Уровень сложности", example = "Легко")
    val difficulty: String,

    @field:Schema(description = "Длительность курса в минутах", example = "90")
    val duration: Int? = null,

    @field:Schema(description = "ID видео в Kinescope для курсов", example = "kinescope_stub_12345")
    val videoUrl: String? = null,

    @field:Schema(description = "Расширенное описание товара")
    val detailedDescription: String? = null,

    @field:Schema(description = "Рекомендации по уходу")
    val careInstructions: String? = null
)

// ============================================================================
// Cart DTOs
// ============================================================================

@Schema(description = "Запрос на добавление товара в корзину")
data class AddToCartRequest(
    @field:NotBlank(message = "ID товара обязателен")
    @field:JsonProperty("goodsId")
    @field:Schema(description = "ID товара для добавления", example = "1", required = true)
    val goodsId: String,

    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    @field:Max(value = 99, message = "Количество не может превышать 99")
    @field:JsonProperty("quantity")
    @field:Schema(description = "Количество единиц товара (1-99)", example = "2", required = true)
    val quantity: Int
)

@Schema(description = "Запрос на изменение количества товара")
data class UpdateQuantityRequest(
    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    @field:Max(value = 99, message = "Количество не может превышать 99")
    @field:JsonProperty("quantity")
    @field:Schema(description = "Новое количество единиц товара (1-99)", example = "3", required = true)
    val quantity: Int
)

@Schema(description = "Элемент корзины с информацией о товаре")
data class CartItemDto(
    @field:JsonProperty("id")
    @field:Schema(description = "ID элемента корзины", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @field:JsonProperty("goods")
    @field:Schema(description = "Полная информация о товаре")
    val goods: GoodsDto,

    @field:JsonProperty("quantity")
    @field:Schema(description = "Количество единиц товара", example = "2")
    val quantity: Int,

    @field:JsonProperty("addedAt")
    @field:Schema(description = "Время добавления в корзину")
    val addedAt: OffsetDateTime
)

@Schema(description = "Сводка корзины покупок")
data class CartSummaryDto(
    @field:JsonProperty("items")
    @field:Schema(description = "Список товаров в корзине")
    val items: List<CartItemDto>,

    @field:JsonProperty("totalItems")
    @field:Schema(description = "Общее количество товаров (сумма всех quantity)", example = "5")
    val totalItems: Int,

    @field:JsonProperty("totalPrice")
    @field:Schema(description = "Итоговая стоимость корзины", example = "4500.00")
    val totalPrice: BigDecimal
)

@Schema(description = "Запрос на объединение локальной корзины (из localStorage) с серверной")
data class MergeCartRequest(
    @field:Valid
    @field:NotNull(message = "Список товаров не может быть null")
    @field:JsonProperty("items")
    @field:Schema(description = "Товары из локальной корзины для синхронизации")
    val items: List<LocalCartItem>
)

@Schema(description = "Элемент локальной корзины из localStorage")
data class LocalCartItem(
    @field:NotBlank(message = "ID товара обязателен")
    @field:JsonProperty("goodsId")
    @field:Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    @field:Max(value = 99, message = "Количество не может превышать 99")
    @field:JsonProperty("quantity")
    @field:Schema(description = "Количество единиц (1-99)", example = "2")
    val quantity: Int
)

// ============================================================================
// Purchase DTOs
// ============================================================================

@Schema(description = "Информация о покупке")
data class PurchaseDto(
    @field:Schema(description = "ID покупки")
    val id: String,

    @field:Schema(description = "ID товара")
    val goodsId: String,

    @field:Schema(description = "Цена покупки")
    val price: BigDecimal,

    @field:Schema(description = "Дата покупки")
    val purchaseDate: OffsetDateTime,

    @field:Schema(description = "Количество")
    val quantity: Int
)

@Schema(description = "Ответ на оформление заказа")
data class CheckoutResponse(
    @field:Schema(description = "ID заказа", example = "550e8400-e29b-41d4-a716-446655440000")
    val orderId: String,

    @field:Schema(description = "Общая стоимость заказа", example = "5400.0")
    val totalPrice: BigDecimal,

    @field:Schema(description = "Список купленных товаров")
    val items: List<PurchasedItem>,

    @field:Schema(description = "Дата и время покупки")
    val purchaseDate: OffsetDateTime
)

@Schema(description = "Купленный товар")
data class PurchasedItem(
    @field:Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @field:Schema(description = "Название товара", example = "Монстера деликатесная")
    val goodsName: String,

    @field:Schema(description = "Количество", example = "2")
    val quantity: Int,

    @field:Schema(description = "Цена за единицу", example = "1500.0")
    val price: BigDecimal
)

// ============================================================================
// Review DTOs
// ============================================================================

@Schema(description = "Запрос на создание отзыва")
data class CreateReviewRequest(
    @field:NotBlank(message = "ID товара обязателен")
    @field:Schema(description = "ID товара", example = "1")
    val goodsId: String,

    @field:Min(value = 1, message = "Рейтинг должен быть минимум 1")
    @field:Max(value = 5, message = "Рейтинг не может превышать 5")
    @field:Schema(description = "Рейтинг от 1 до 5", example = "5")
    val rating: Int,

    @field:NotBlank(message = "Комментарий обязателен")
    @field:Size(min = 10, max = 1000, message = "Комментарий должен содержать от 10 до 1000 символов")
    @field:Schema(
        description = "Текст отзыва (10-1000 символов)",
        example = "Отличное растение! Быстро прижилось и радует меня каждый день."
    )
    val comment: String
)

@Schema(description = "Отзыв о товаре")
data class ReviewDto(
    @field:Schema(description = "ID товара")
    val goodsId: String,

    @field:Schema(description = "ID пользователя")
    val userId: String,

    @field:Schema(description = "Имя автора")
    val userName: String,

    @field:Schema(description = "Рейтинг от 1 до 5")
    val rating: Int,

    @field:Schema(description = "Текст отзыва")
    val comment: String,

    @field:Schema(description = "Дата создания")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "Дата обновления")
    val updatedAt: OffsetDateTime
)

@Schema(description = "Средний рейтинг товара")
data class GoodsRatingDto(
    @field:Schema(description = "Средний рейтинг (0.0 если нет отзывов)", example = "4.5")
    val averageRating: Double,

    @field:Schema(description = "Количество отзывов", example = "12")
    val totalReviews: Int
)

// ============================================================================
// DTO Mappers (Domain Model → DTO)
// ============================================================================

// User mappers
fun User.toUserDto() = UserDto(
    id = id ?: throw IllegalStateException("User must have an ID"),
    name = name,
    email = email,
    canPurchase = roles.contains(UserRole.BUYER),
    isAdmin = if (roles.contains(UserRole.ADMIN)) true else null
)

// Category mapper
fun Category.toCategoryDto() = CategoryDto(
    id = id,
    name = name,
    type = type
)

// Media mapper
fun Media.toMediaDto(): MediaDto = when (this) {
    is Image -> ImageDto(url = url, order = order)
    is Video -> VideoDto(url = url, order = order)
}

// Goods mapper
fun Goods.toGoodsDto(category: Category?) = GoodsDto(
    id = id ?: throw IllegalStateException("Goods must have an ID"),
    name = name,
    description = description,
    price = price.toDouble(),
    media = media.map { it.toMediaDto() }.sortedBy { it.order },
    category = category?.toCategoryDto(),
    difficulty = difficulty,
    duration = duration,
    videoUrl = videoUrl,
    detailedDescription = detailedDescription,
    careInstructions = careInstructions
)

// Purchase mapper
fun Purchase.toPurchaseDto() = PurchaseDto(
    id = id ?: throw IllegalStateException("Purchase must have an ID"),
    goodsId = goodsId,
    price = price,
    purchaseDate = purchaseDate,
    quantity = quantity
)

// Review mapper
fun Review.toReviewDto() = ReviewDto(
    goodsId = goodsId,
    userId = userId,
    userName = userName,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// Cart mappers
fun CartItemWithGoods.toCartItemDto() = CartItemDto(
    id = id,
    goods = goods.toGoodsDto(category),
    quantity = quantity,
    addedAt = addedAt
)

fun CartSummary.toCartSummaryDto() = CartSummaryDto(
    items = items.map { it.toCartItemDto() },
    totalItems = totalItems,
    totalPrice = totalPrice
)

// ============================================================================
// Admin - Goods Management DTOs
// ============================================================================

@Schema(description = "Запрос на создание товара (только для администраторов)")
data class CreateGoodsRequest(
    @field:NotBlank(message = "Название обязательно")
    @field:Size(min = 3, max = 500, message = "Название должно содержать от 3 до 500 символов")
    @field:Schema(description = "Название товара", example = "Монстера деликатесная")
    val name: String,

    @field:NotBlank(message = "Описание обязательно")
    @field:Size(min = 10, max = 2000, message = "Описание должно содержать от 10 до 2000 символов")
    @field:Schema(
        description = "Краткое описание товара",
        example = "Популярная тропическая лиана с крупными резными листьями"
    )
    val description: String,

    @field:DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
    @field:Schema(description = "Цена товара в рублях", example = "1500.0")
    val price: BigDecimal,

    @field:NotBlank(message = "Категория обязательна")
    @field:Schema(description = "ID категории товара")
    val categoryId: String,

    @field:Size(max = 100, message = "Поле difficulty не может превышать 100 символов")
    @field:Schema(description = "Уровень сложности", example = "Легко")
    val difficulty: String? = null,

    @field:Min(value = 1, message = "Длительность должна быть минимум 1 минута")
    @field:Schema(description = "Длительность курса в минутах (только для курсов)", example = "90")
    val duration: Int? = null,

    @field:Size(max = 500, message = "URL видео не может превышать 500 символов")
    @field:Schema(description = "URL видео в Kinescope (только для курсов)")
    val videoUrl: String? = null,

    @field:Size(max = 10000, message = "Детальное описание не может превышать 10000 символов")
    @field:Schema(description = "Расширенное описание товара (поддерживает Markdown)")
    val detailedDescription: String? = null,

    @field:Size(max = 5000, message = "Инструкции по уходу не могут превышать 5000 символов")
    @field:Schema(description = "Рекомендации по уходу (поддерживает Markdown)")
    val careInstructions: String? = null
)

// ============================================================================
// Pagination DTOs
// ============================================================================

@Schema(description = "Постраничный ответ со списком товаров")
data class PagedGoodsResponse(
    @field:Schema(description = "Список товаров на текущей странице")
    val content: List<GoodsDto>,

    @field:Schema(description = "Номер текущей страницы (с 0)")
    val page: Int,

    @field:Schema(description = "Размер страницы")
    val size: Int,

    @field:Schema(description = "Общее количество товаров")
    val totalElements: Long,

    @field:Schema(description = "Общее количество страниц")
    val totalPages: Int,

    @field:Schema(description = "Есть ли следующая страница")
    val hasNext: Boolean,

    @field:Schema(description = "Есть ли предыдущая страница")
    val hasPrevious: Boolean
)

@Schema(description = "Запрос на обновление товара (только для администраторов)")
data class UpdateGoodsRequest(
    @field:NotBlank(message = "Название обязательно")
    @field:Size(min = 3, max = 500, message = "Название должно содержать от 3 до 500 символов")
    @field:Schema(description = "Название товара", example = "Монстера деликатесная")
    val name: String,

    @field:NotBlank(message = "Описание обязательно")
    @field:Size(min = 10, max = 2000, message = "Описание должно содержать от 10 до 2000 символов")
    @field:Schema(
        description = "Краткое описание товара",
        example = "Популярная тропическая лиана с крупными резными листьями"
    )
    val description: String,

    @field:DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
    @field:Schema(description = "Цена товара в рублях", example = "1500.0")
    val price: BigDecimal,

    @field:NotBlank(message = "Категория обязательна")
    @field:Schema(description = "ID категории товара")
    val categoryId: String,

    @field:Size(max = 100, message = "Поле difficulty не может превышать 100 символов")
    @field:Schema(description = "Уровень сложности", example = "Легко")
    val difficulty: String? = null,

    @field:Min(value = 1, message = "Длительность должна быть минимум 1 минута")
    @field:Schema(description = "Длительность курса в минутах (только для курсов)", example = "90")
    val duration: Int? = null,

    @field:Size(max = 500, message = "URL видео не может превышать 500 символов")
    @field:Schema(description = "URL видео в Kinescope (только для курсов)")
    val videoUrl: String? = null,

    @field:Size(max = 10000, message = "Детальное описание не может превышать 10000 символов")
    @field:Schema(description = "Расширенное описание товара (поддерживает Markdown)")
    val detailedDescription: String? = null,

    @field:Size(max = 5000, message = "Инструкции по уходу не могут превышать 5000 символов")
    @field:Schema(description = "Рекомендации по уходу (поддерживает Markdown)")
    val careInstructions: String? = null
)
