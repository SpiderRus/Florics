package com.example.webflux.service

import com.example.webflux.repository.AiConversationRepository
import com.example.webflux.service.aibot.AiBotServiceException
import com.example.webflux.service.aibot.AiBotTimeoutException
import com.example.webflux.service.aibot.ConversationAccessDeniedException
import com.example.webflux.service.aibot.ConversationNotFoundException
import com.example.webflux.service.aibot.dto.*
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * Реактивный сервис для взаимодействия с AI Agent через WebClient
 *
 * Предоставляет suspend функции для всех операций с AI чат-ботом:
 * - Управление разговорами (создание, список, получение, удаление)
 * - Получение истории сообщений
 * - Отправка сообщений и получение ответов от AI
 *
 * Реализует изоляцию разговоров между пользователями через AiConversationRepository.
 */
@Service
class AiBotService(
    @param:Qualifier("aiAgentWebClient") private val webClient: WebClient,
    private val conversationRepository: AiConversationRepository
) {
    /**
     * Создать новый разговор с AI ассистентом
     *
     * @param userId ID пользователя-владельца
     * @param title Название разговора
     * @return Созданный разговор
     * @throws AiBotServiceException при ошибке связи с AI Agent
     */
    suspend fun createConversation(userId: String, title: String, context: String?): ConversationResponse {
        logger.debug("Creating conversation for user {}: {}", userId, title)

        val request = ConversationCreateRequest(title, context)

        return try {
                webClient.post()
                    .uri("/conversations")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus({ it.is4xxClientError }, errorHandler4xx)
                    .onStatus({ it.is5xxServerError }, errorHandler5xx)
                    .awaitBody<ConversationResponse>()
            } catch (e: WebClientRequestException) {
                logger.error("Failed to connect to AI Agent service", e)
                throw AiBotServiceException("Failed to connect to AI Agent service", e)
            } catch (e: TimeoutException) {
                logger.error("AI Agent request timed out", e)
                throw AiBotTimeoutException("AI Agent request timed out", e)
            }.also { logger.debug("Conversation created: {}", it.id) }
    }

    /**
     * Получить список всех разговоров пользователя
     *
     * @param userId ID пользователя
     * @return Список разговоров пользователя
     * @throws AiBotServiceException при ошибке связи с AI Agent
     */
    suspend fun listUserConversations(userId: String): List<ConversationResponse> {
        logger.debug("Listing conversations for user {}", userId)

        // Получить список conversationIds пользователя
        val conversationIds = conversationRepository.findByUserId(userId)

        if (conversationIds.isEmpty()) {
            logger.debug("No conversations found for user {}", userId)
            return emptyList()
        }

        // Для каждого ID получить детали (может быть удален в AI Agent)
        return conversationIds.mapNotNull { id ->
            try {
                webClient.get()
                    .uri("/conversations/{id}", id)
                    .retrieve()
                    .awaitBody<ConversationResponse>()
            } catch (e: Exception) {
                logger.warn("Failed to fetch conversation {}: {}", id, e.message)
                // Conversation мог быть удален в AI Agent, но маппинг остался
                // Удаляем устаревший маппинг
                conversationRepository.deleteByConversationId(id).let { null }
            }
        }
    }

    /**
     * Получить детали разговора
     *
     * @param userId ID пользователя
     * @param conversationId UUID разговора
     * @return Детали разговора
     * @throws ConversationNotFoundException если разговор не найден
     * @throws ConversationAccessDeniedException если пользователь не владелец
     * @throws AiBotServiceException при ошибке связи с AI Agent
     */
    suspend fun getConversation(userId: String, conversationId: String): ConversationResponse {
        logger.debug("Getting conversation {} for user {}", conversationId, userId)
        validateOwnership(userId, conversationId)

        return try {
            webClient.get()
                .uri("/conversations/{id}", conversationId)
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .awaitBody<ConversationResponse>()
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI Agent service", e)
            throw AiBotServiceException("Failed to connect to AI Agent service", e)
        } catch (e: TimeoutException) {
            logger.error("AI Agent request timed out", e)
            throw AiBotTimeoutException("AI Agent request timed out", e)
        }
    }

    /**
     * Получить историю сообщений разговора
     *
     * @param userId ID пользователя
     * @param conversationId UUID разговора
     * @param limit Максимальное количество сообщений (по умолчанию 50)
     * @return Список сообщений в хронологическом порядке
     * @throws ConversationNotFoundException если разговор не найден
     * @throws ConversationAccessDeniedException если пользователь не владелец
     * @throws AiBotServiceException при ошибке связи с AI Agent
     */
    suspend fun getMessages(userId: String, conversationId: String, limit: Int = 50): List<MessageResponse> {
        logger.debug("Getting messages for conversation {} (user {}, limit {})", conversationId, userId, limit)
        validateOwnership(userId, conversationId)

        return try {
            webClient.get()
                .uri { builder ->
                    builder.path("/conversations/{id}/messages")
                        .queryParam("limit", limit)
                        .build(conversationId)
                }
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .bodyToFlow<MessageResponse>()
                .toList()
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI Agent service", e)
            throw AiBotServiceException("Failed to connect to AI Agent service", e)
        } catch (e: TimeoutException) {
            logger.error("AI Agent request timed out", e)
            throw AiBotTimeoutException("AI Agent request timed out", e)
        }
    }

    /**
     * Удалить разговор
     *
     * Удаляет разговор в AI Agent и маппинг в локальном репозитории.
     *
     * @param userId ID пользователя
     * @param conversationId UUID разговора
     * @throws ConversationNotFoundException если разговор не найден
     * @throws ConversationAccessDeniedException если пользователь не владелец
     * @throws AiBotServiceException при ошибке связи с AI Agent
     */
    suspend fun deleteConversation(userId: String, conversationId: String) {
        logger.debug("Deleting conversation {} for user {}", conversationId, userId)
        validateOwnership(userId, conversationId)

        try {
            webClient.delete()
                .uri("/conversations/{id}", conversationId)
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .awaitBodilessEntity()
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI Agent service", e)
            throw AiBotServiceException("Failed to connect to AI Agent service", e)
        } catch (e: TimeoutException) {
            logger.error("AI Agent request timed out", e)
            throw AiBotTimeoutException("AI Agent request timed out", e)
        }

        // Удалить маппинг
        conversationRepository.deleteByConversationId(conversationId).also { logger.debug("Conversation {} deleted", conversationId) }
    }

    /**
     * Отправить сообщение в чат и получить ответ от AI
     *
     * @param userId ID пользователя
     * @param conversationId UUID разговора
     * @param message Сообщение
     * @param useRag Флаг включения RAG (Retrieval-Augmented Generation).
     *               true = искать контекст в документах, false = только базовые знания LLM
     *               По умолчанию: true
     * @return Ответ от AI с контекстом (если RAG включен)
     * @throws ConversationNotFoundException если разговор не найден
     * @throws ConversationAccessDeniedException если пользователь не владелец
     * @throws AiBotServiceException при ошибке связи с AI Agent
     */
    suspend fun sendMessage(
        userId: String,
        conversationId: String,
        message: String,
        useRag: Boolean = true
    ): ChatResponse {
        logger.debug("Sending message to conversation {} for user {} (RAG: {}): {}",
            conversationId, userId, useRag, message)
        validateOwnership(userId, conversationId)

        return try {
            webClient.post()
                .uri("/chat/{conversationId}", conversationId)
                .bodyValue(ChatRequest(message, useRag))
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .awaitBody<ChatResponse>()
                .also {
                    logger.debug("Received AI response for conversation {}: {} chars", conversationId, it.response.length)
                }
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI Agent service", e)
            throw AiBotServiceException("Failed to connect to AI Agent service", e)
        } catch (e: TimeoutException) {
            logger.error("AI Agent request timed out", e)
            throw AiBotTimeoutException("AI Agent request timed out", e)
        }
    }

    /**
     * Валидация владения разговором
     *
     * @param userId ID пользователя
     * @param conversationId UUID разговора
     * @throws ConversationNotFoundException если разговор не найден в локальном маппинге
     * @throws ConversationAccessDeniedException если пользователь не владелец
     */
    private suspend fun validateOwnership(userId: String, conversationId: String) {
        val ownerId = conversationRepository.findByConversationId(conversationId)
            ?: throw ConversationNotFoundException(conversationId)

        if (ownerId != userId) {
            logger.warn("User {} attempted to access conversation {} owned by user {}", userId, conversationId, ownerId)
            throw ConversationAccessDeniedException(conversationId)
        }
    }

    private companion object {
        /**
         * Error handler для 4xx ошибок
         */
        private val errorHandler4xx: (ClientResponse) -> Mono<out Throwable> = { response ->
            response.bodyToMono<String>().map { body ->
                when (response.statusCode().value()) {
                    404 -> AiBotServiceException("Resource not found in AI Agent")
                    400 -> AiBotServiceException("Bad request to AI Agent: $body")
                    else -> AiBotServiceException("AI Agent client error (${response.statusCode()}): $body")
                }
            }.defaultIfEmpty(AiBotServiceException("AI Agent client error: ${response.statusCode()}"))
        }

        /**
         * Error handler для 5xx ошибок
         */
        private val errorHandler5xx: (ClientResponse) -> Mono<out Throwable> = { response ->
            response.bodyToMono<String>().map { body ->
                AiBotServiceException("AI Agent server error (${response.statusCode()}): $body")
            }.defaultIfEmpty(AiBotServiceException("AI Agent server error: ${response.statusCode()}"))
        }

        private val logger = LoggerFactory.getLogger(AiBotService::class.java)
    }
}
