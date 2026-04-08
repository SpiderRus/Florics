package com.example.webflux.controller

import com.example.webflux.repository.AiConversationRepository
import com.example.webflux.repository.CategoryRepository
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.service.AiBotService
import com.example.webflux.service.aibot.dto.ConversationResponse
import com.example.webflux.service.aibot.dto.MessageResponse
import com.example.webflux.service.aibot.dto.ChatResponse
import com.example.webflux.security.SecurityUtils
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST контроллер для AI чат-бота
 *
 * Предоставляет API для взаимодействия с AI консультантом на страницах товаров.
 * Управляет разговорами (conversations), привязанными к товарам (goods).
 *
 * Каждый пользователь может иметь отдельный conversation для каждого товара.
 * Все операции требуют аутентификации.
 */
@RestController
@RequestMapping("/api/aibot")
class AiBotController(
    private val aiBotService: AiBotService,
    private val conversationRepository: AiConversationRepository,
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AiBotController::class.java)
    }

    /**
     * Создать или получить conversation для товара
     *
     * Если у пользователя уже есть conversation для данного товара, вернет существующий.
     * Иначе создаст новый conversation с названием "Вопросы о товаре: {goodsName}".
     *
     * @param request Тело запроса с goodsId и goodsName
     * @return HTTP 200 OK с данными conversation
     */
    @PostMapping("/conversations")
    suspend fun createOrGetConversation(
        @RequestBody request: CreateConversationRequest
    ): ResponseEntity<ConversationResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting conversation for goods {}", userId, request.goodsId)

        // Проверить, есть ли уже conversation для этого товара
        val existingConversationId = conversationRepository.findConversationByUserAndGoods(userId, request.goodsId)

        val conversation = if (existingConversationId != null) {
            logger.debug("Found existing conversation {} for user {} and goods {}", existingConversationId, userId, request.goodsId)
            aiBotService.getConversation(userId, existingConversationId)
        } else {
            val goods = goodsRepository.findById(request.goodsId);
            val category = goods?.let { categoryRepository.findById(it.categoryId) }

            // Создать новый conversation
            val context = """
                    Товар: ${request.goodsName}
                    
                    ${category?.let { "Категория товара: " + it.name } ?: ""}
                     
                    Описание товара:
                    ${goods?.detailedDescription ?: (goods?.description ?: "")}
                    """
            logger.debug("Creating new conversation for user {} and goods {}: {}", userId, request.goodsId, request.goodsName)
            val newConversation = aiBotService.createConversation(userId, request.goodsName, context)

            // Сохранить маппинг (userId, goodsId) → conversationId
            conversationRepository.saveGoodsConversation(userId, request.goodsId, newConversation.id)
            logger.debug("Saved goods conversation mapping: ({}, {}) -> {}", userId, request.goodsId, newConversation.id)

            newConversation
        }

        return ResponseEntity.ok(conversation)
    }

    /**
     * Получить conversation для товара
     *
     * @param goodsId ID товара
     * @return HTTP 200 OK с данными conversation или 404 если не найдено
     */
    @GetMapping("/conversations/by-goods/{goodsId}")
    suspend fun getConversationByGoods(
        @PathVariable goodsId: String
    ): ResponseEntity<ConversationResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting conversation for goods {}", userId, goodsId)

        val conversationId = conversationRepository.findConversationByUserAndGoods(userId, goodsId)
            ?: return ResponseEntity.notFound().build()

        val conversation = aiBotService.getConversation(userId, conversationId)
        return ResponseEntity.ok(conversation)
    }

    /**
     * Получить историю сообщений conversation
     *
     * @param conversationId UUID разговора
     * @param limit Максимальное количество сообщений (по умолчанию 50)
     * @return HTTP 200 OK со списком сообщений
     */
    @GetMapping("/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @PathVariable conversationId: String,
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<MessageResponse>> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting messages for conversation {} (limit: {})", userId, conversationId, limit)

        val messages = aiBotService.getMessages(userId, conversationId, limit)
        return ResponseEntity.ok(messages)
    }

    /**
     * Отправить сообщение в чат и получить ответ от AI
     *
     * Всегда использует useRag=true для получения контекста из векторной БД.
     *
     * @param conversationId UUID разговора
     * @param request Тело запроса с сообщением пользователя
     * @return HTTP 200 OK с ответом AI
     */
    @PostMapping("/chat/{conversationId}")
    suspend fun sendMessage(
        @PathVariable conversationId: String,
        @RequestBody request: ChatMessageRequest
    ): ResponseEntity<ChatResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} sending message to conversation {}: {}", userId, conversationId, request.message)

        val response = aiBotService.sendMessage(
            userId = userId,
            conversationId = conversationId,
            message = request.message,
            useRag = true  // Всегда использовать RAG для контекста о товарах
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Удалить conversation (очистить историю)
     *
     * @param conversationId UUID разговора для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/conversations/{conversationId}")
    suspend fun deleteConversation(
        @PathVariable conversationId: String
    ): ResponseEntity<Void> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} deleting conversation {}", userId, conversationId)

        aiBotService.deleteConversation(userId, conversationId)
        return ResponseEntity.noContent().build()
    }

    /**
     * DTO для создания conversation для товара
     */
    data class CreateConversationRequest(
        val goodsId: String,
        val goodsName: String
    )

    /**
     * DTO для отправки сообщения в чат
     */
    data class ChatMessageRequest(
        val message: String
    )
}
