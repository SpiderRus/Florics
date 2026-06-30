package com.example.webflux.controller

import com.example.webflux.repository.AiConversationRepository
import com.example.webflux.repository.CategoryRepository
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.service.AiBotService
import com.example.webflux.service.aibot.dto.ChatResponse
import com.example.webflux.service.aibot.dto.ConversationResponse
import com.example.webflux.service.aibot.dto.FlorariumChunk
import com.example.webflux.service.aibot.dto.MessageResponse
import com.example.webflux.service.aibot.dto.TokenChunk
import com.example.webflux.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.UUID

/**
 * REST контроллер для AI чат-бота.
 *
 * Предоставляет API для взаимодействия с AI консультантом на страницах товаров.
 * Контракт /api/aibot стабилен для фронтенда; за ним стоит клиент бота OllamaTestController
 * (см. [AiBotService]). Каждый пользователь имеет отдельный разговор для каждого товара.
 * Все операции требуют роли BUYER.
 */
@RestController
@RequestMapping("/api/aibot")
class AiBotController(
    private val aiBotService: AiBotService,
    private val conversationRepository: AiConversationRepository,
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * Создать или получить conversation для товара.
     *
     * Если у пользователя уже есть разговор для товара — вернёт его (chatId из маппинга).
     * Иначе сгенерирует новый chatId. В обоих случаях гарантирует существование сессии в боте
     * (идемпотентно). Контекст товара передаётся боту через topic (уходит в системный промпт).
     */
    @PostMapping("/conversations")
    @PreAuthorize("hasRole('BUYER')")
    suspend fun createOrGetConversation(
        @RequestBody request: CreateConversationRequest
    ): ResponseEntity<ConversationResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting conversation for goods {}", userId, request.goodsId)

        val existingChatId = conversationRepository.findConversationByUserAndGoods(userId, request.goodsId)
        val chatId = existingChatId ?: UUID.randomUUID().toString()

        // Контекст товара -> topic бота
        val goods = goodsRepository.findById(request.goodsId)
        val category = goods?.let { categoryRepository.findById(it.categoryId) }
        // Топик = только название товара (короткое ограничение темы + RAG-компрессия);
        // остальной контекст (категория, описание, призыв к покупке) — в description.
        val topic = request.goodsName
        val description = buildGoodsContext(category?.name, goods?.detailedDescription ?: goods?.description)

        // Гарантировать сессию в боте (идемпотентно)
        aiBotService.ensureConversation(chatId, topic, description)

        // Для нового разговора — сохранить маппинг (userId, goodsId) -> chatId
        if (existingChatId == null) {
            conversationRepository.saveOrCreateGoodsConversation(userId, request.goodsId, chatId)
            logger.debug("Saved goods conversation mapping: ({}, {}) -> {}", userId, request.goodsId, chatId)
        }

        return ResponseEntity.ok(synthesizeConversation(chatId, request.goodsName))
    }

    /**
     * Получить conversation для товара (или 404, если не создан).
     */
    @GetMapping("/conversations/by-goods/{goodsId}")
    @PreAuthorize("hasRole('BUYER')")
    suspend fun getConversationByGoods(
        @PathVariable goodsId: String
    ): ResponseEntity<ConversationResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting conversation for goods {}", userId, goodsId)

        val chatId = conversationRepository.findConversationByUserAndGoods(userId, goodsId)
            ?: return ResponseEntity.notFound().build()

        val goodsName = goodsRepository.findById(goodsId)?.name ?: ""
        return ResponseEntity.ok(synthesizeConversation(chatId, goodsName))
    }

    /**
     * Получить историю сообщений conversation.
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @PathVariable conversationId: String,
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<MessageResponse>> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting messages for conversation {} (limit: {})", userId, conversationId, limit)

        return ResponseEntity.ok(aiBotService.getMessages(userId, conversationId, limit))
    }

    /**
     * Отправить сообщение и получить ответ ПОТОКОМ (SSE).
     *
     * Каждое событие SSE несёт JSON-токен ({"t":"..."}) — это сохраняет ведущие пробелы
     * в токенах LLM (иначе SSE склеивает слова). Фронт склеивает t-фрагменты в ответ.
     */
    @PostMapping("/chat/{conversationId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @PreAuthorize("hasRole('BUYER')")
    suspend fun streamMessage(
        @PathVariable conversationId: String,
        @RequestBody request: ChatMessageRequest
    ): Flow<TokenChunk> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} streaming message to conversation {}", userId, conversationId)

        return aiBotService.streamMessage(userId, conversationId, request.message)
            .map { TokenChunk(it) }
    }

    /**
     * Отправить сообщение и получить ответ целиком (нестримовый fallback).
     */
    @PostMapping("/chat/{conversationId}")
    @PreAuthorize("hasRole('BUYER')")
    suspend fun sendMessage(
        @PathVariable conversationId: String,
        @RequestBody request: ChatMessageRequest
    ): ResponseEntity<ChatResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} sending message to conversation {}", userId, conversationId)

        return ResponseEntity.ok(aiBotService.sendMessage(userId, conversationId, request.message))
    }

    /**
     * «Очистить историю»: удаляет локальный маппинг (у бота нет удаления).
     */
    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('BUYER')")
    suspend fun deleteConversation(
        @PathVariable conversationId: String
    ): ResponseEntity<Void> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} deleting conversation {}", userId, conversationId)

        return aiBotService.deleteConversation(userId, conversationId).let { ResponseEntity.noContent().build() }
    }

    // ===== Дизайнер флорариумов (agentType=florarium, генерация картинок) — страница /custom-terrarium =====

    /**
     * Загрузить последний разговор дизайнера флорариумов (или создать первый).
     *
     * Вызывается при открытии страницы: восстанавливаем самый свежий florarium-разговор пользователя,
     * чтобы продолжить диалог (бот помнит контекст и последнюю картинку). Если разговоров ещё нет —
     * создаём новый. Гарантирует существование сессии в боте (идемпотентно).
     */
    @PostMapping("/florarium/conversations")
    @PreAuthorize("hasRole('BUYER')")
    suspend fun createOrGetFlorariumConversation(): ResponseEntity<ConversationResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} requesting latest florarium conversation", userId)

        val existingChatId = conversationRepository.findLatestFlorariumConversationByUser(userId)
        val chatId = if (existingChatId != null) {
            // Защита от потери сессии в боте — пересоздание идемпотентно
            aiBotService.ensureFlorariumConversation(existingChatId, null)
            existingChatId
        } else {
            newFlorariumConversation(userId)
        }

        return ResponseEntity.ok(synthesizeConversation(chatId, "Флорариум под заказ"))
    }

    /**
     * «Закончить разговор»: создать новый florarium-разговор.
     *
     * Старый разговор остаётся в истории (последующая загрузка страницы вернёт уже этот, новый).
     */
    @PostMapping("/florarium/conversations/new")
    @PreAuthorize("hasRole('BUYER')")
    suspend fun createNewFlorariumConversation(): ResponseEntity<ConversationResponse> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} starting new florarium conversation", userId)

        val chatId = newFlorariumConversation(userId)
        return ResponseEntity.ok(synthesizeConversation(chatId, "Флорариум под заказ"))
    }

    /** Создать новый florarium-разговор: сгенерировать chatId, создать сессию в боте, сохранить маппинг. */
    private suspend fun newFlorariumConversation(userId: String): String {
        val chatId = UUID.randomUUID().toString()
        // Topic не нужен — системный промпт florarium задан на стороне бота
        aiBotService.ensureFlorariumConversation(chatId, null)
        conversationRepository.saveFlorariumConversation(userId, chatId)
        logger.debug("Created new florarium conversation {} for user {}", chatId, userId)
        return chatId
    }

    /**
     * Стриминг ответа дизайнера флорариумов (SSE): текст и/или картинки.
     *
     * Каждое событие SSE — JSON [FlorariumChunk] ({"type":"text",...} или {"type":"image","imageUrl":...}).
     * Картинки приходят ссылкой на backend-прокси (см. [florariumImage]).
     */
    @PostMapping("/florarium/chat/{conversationId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @PreAuthorize("hasRole('BUYER')")
    suspend fun streamFlorarium(
        @PathVariable conversationId: String,
        @RequestBody request: ChatMessageRequest
    ): Flow<FlorariumChunk> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} streaming florarium message to conversation {}", userId, conversationId)

        return aiBotService.streamFlorariumMessage(userId, conversationId, request.message)
    }

    /**
     * Прокси сгенерированной картинки флорариума (тянет байты из бота, отдаёт под ролью BUYER).
     *
     * Картинки бота недоступны браузеру напрямую (другой порт, нет CORS), поэтому проксируем здесь.
     * Фронт грузит картинку авторизованным fetch (Bearer) → blob → <img>.
     */
    @GetMapping("/florarium/images/{imageId}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    suspend fun florariumImage(@PathVariable imageId: String): ResponseEntity<Flux<DataBuffer>> {
        val userId = SecurityUtils.requireCurrentUserId()
        logger.debug("User {} fetching florarium image {}", userId, imageId)

        return aiBotService.proxyImage(imageId).awaitSingle()
    }

    /**
     * Собрать контекст-описание товара для бота (категория + описание + призыв к покупке).
     * Уходит в поле description сессии (подмешивается в системный промпт, но НЕ в RAG-компрессию;
     * название товара передаётся отдельно через topic). Описание усекается, чтобы не раздувать промпт.
     */
    private fun buildGoodsContext(categoryName: String?, description: String?): String {
        val sb = StringBuilder()
        sb.append("ВАЖНО!!! На вопрос о покупке товара, предлагай купить на этом сайте, добавив товар в корзину!\n\n")

        if (!categoryName.isNullOrBlank()) sb.append("Категория: $categoryName\n")
        if (!description.isNullOrBlank()) {
            val trimmed = description.trim().take(MAX_DESCRIPTION_CHARS)
            sb.append("Описание: $trimmed\n")
        }
        return sb.toString()
    }

    private fun synthesizeConversation(chatId: String, goodsName: String): ConversationResponse {
        val now = OffsetDateTime.now()
        return ConversationResponse(
            id = chatId,
            title = goodsName,
            createdAt = now,
            updatedAt = now,
            messageCount = 0
        )
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

    companion object {
        private const val MAX_DESCRIPTION_CHARS = 2000
        private val logger = LoggerFactory.getLogger(AiBotController::class.java)
    }
}
