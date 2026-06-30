package com.example.webflux.service

import com.example.webflux.config.AiAgentProperties
import com.example.webflux.domain.model.Purchase
import com.example.webflux.repository.PurchaseRepository
import com.example.webflux.repository.UserRepository
import com.example.webflux.service.aibot.dto.MessageResponse
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

/**
 * Заказ кастомного флорариума, обогащённый данными пользователя — для экрана админки.
 */
data class CustomOrderView(
    val order: Purchase,
    val userName: String?,
    val userEmail: String?
)

/**
 * Экспертный чат мастера по заказу: id экспертной сессии, история сообщений и картинки дизайна.
 *
 * [designImages] — все картинки исходного разговора-дизайнера (для карусели в админке),
 * [selectedImageUrl] — выбранная в заказе картинка (активная в карусели).
 */
data class ExpertChatView(
    val conversationId: String,
    val messages: List<MessageResponse>,
    val designImages: List<String>,
    val selectedImageUrl: String?
)

/**
 * Управление кастомными заказами флорариумов на стороне администратора:
 * просмотр всех заказов, проставление цены/статуса и чат-консультация мастера с экспертом.
 */
@Service
class CustomOrderService(
    private val purchaseRepository: PurchaseRepository,
    private val userRepository: UserRepository,
    private val aiBotService: AiBotService,
    private val aiAgentProperties: AiAgentProperties,
    private val objectMapper: ObjectMapper
) {
    /**
     * Все кастомные заказы с именем/email пользователя (для таблицы в админке).
     */
    suspend fun listAllOrders(): List<CustomOrderView> =
        purchaseRepository.findAllCustomOrders()
            .map { order ->
                val user = userRepository.findById(order.userId)
                CustomOrderView(order = order, userName = user?.name, userEmail = user?.email)
            }
            .toList()

    /**
     * Проставить цену и/или статус заказа (действие администратора).
     * Возвращает обновлённый заказ, обогащённый данными пользователя.
     */
    suspend fun updateOrder(id: String, price: BigDecimal?, status: String?): CustomOrderView {
        val existing = purchaseRepository.findById(id)
            ?: throw IllegalArgumentException("Order not found: $id")
        require(existing.conversationId != null) { "Not a custom florarium order: $id" }
        if (status != null) {
            require(status in ALLOWED_STATUSES) { "Invalid status: $status" }
        }
        price?.let { require(it >= BigDecimal.ZERO) { "Price must be non-negative" } }

        val updated = purchaseRepository.update(
            existing.copy(
                price = price ?: existing.price,
                status = status ?: existing.status
            )
        )
        val user = userRepository.findById(updated.userId)
        return CustomOrderView(order = updated, userName = user?.name, userEmail = user?.email)
    }

    /**
     * Открыть/восстановить экспертный чат мастера по заказу.
     *
     * Гарантирует сессию florarium-expert в боте (topic = id исходного разговора + url картинки заказа)
     * и возвращает текущую историю. chatId детерминирован от id заказа (отдельный чат на заказ).
     */
    suspend fun openExpertChat(orderId: String): ExpertChatView {
        val order = loadCustomOrder(orderId)
        val chatId = expertChatId(orderId)

        // topic для florarium-expert — JSON {"sourceChatId","imageUrl"} (см. контракт AIAgentNew)
        val topic = objectMapper.writeValueAsString(
            mapOf(
                "sourceChatId" to order.conversationId,
                "imageUrl" to toBotImageUrl(order.imageUrl!!)
            )
        )
        aiBotService.ensureFlorariumExpertConversation(chatId, topic)

        // Картинки исходного разговора-дизайнера для карусели; гарантируем наличие выбранной картинки
        val selected = order.imageUrl!!
        val designImages = aiBotService.getConversationImages(order.conversationId!!)
            .let { if (it.contains(selected)) it else listOf(selected) + it }

        return ExpertChatView(
            conversationId = chatId,
            messages = aiBotService.getExpertMessages(chatId),
            designImages = designImages,
            selectedImageUrl = selected
        )
    }

    /**
     * Стрим ответа эксперта на сообщение мастера (текст). Сессия должна быть открыта через [openExpertChat].
     */
    suspend fun streamExpert(orderId: String, message: String): Flow<String> {
        loadCustomOrder(orderId) // проверка, что заказ существует и кастомный
        return aiBotService.streamFlorariumExpertMessage(expertChatId(orderId), message)
    }

    /** Загрузить кастомный заказ с проверкой, что у него есть разговор и картинка. */
    private suspend fun loadCustomOrder(orderId: String): Purchase {
        val order = purchaseRepository.findById(orderId)
            ?: throw IllegalArgumentException("Order not found: $orderId")
        require(order.conversationId != null && order.imageUrl != null) {
            "Not a custom florarium order with design: $orderId"
        }
        return order
    }

    /** Детерминированный id экспертной сессии для заказа (валидный UUID, стабильный). */
    private fun expertChatId(orderId: String): String =
        UUID.nameUUIDFromBytes("florarium-expert:$orderId".toByteArray()).toString()

    /** Прокси-URL Florics (/api/aibot/florarium/images/<uuid>) -> внутренний путь бота (/api/v1/ollama/images/<uuid>). */
    private fun toBotImageUrl(proxyUrl: String): String =
        aiAgentProperties.basePath + "/images/" + proxyUrl.substringAfterLast('/')

    private companion object {
        val ALLOWED_STATUSES = setOf("NEW", "IN_PROGRESS", "DONE", "CANCELLED")
    }
}
