package com.example.webflux.service

import com.example.webflux.config.AiAgentProperties
import com.example.webflux.repository.AiConversationRepository
import com.example.webflux.service.aibot.AiBotServiceException
import com.example.webflux.service.aibot.AiBotTimeoutException
import com.example.webflux.service.aibot.ConversationAccessDeniedException
import com.example.webflux.service.aibot.ConversationNotFoundException
import com.example.webflux.service.aibot.dto.*
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * Реактивный клиент чат-бота OllamaTestController (проект AIAgentNew) через WebClient.
 *
 * Бот хранит историю сам (по chatId), делает RAG автоматически и умеет стриминг (SSE).
 * Контракт бота (base = .../api/v1/ollama):
 *  - POST /chat                     — создать сессию {agentType, chatId, topic}, идемпотентно
 *  - POST /{chatId}/chat/stream     — стриминг ответа (тело — сырой текст), SSE токенов
 *  - POST /{chatId}/chat            — ответ целиком (тело — сырой текст) → String
 *  - GET  /{chatId}/chat            — история сообщений
 *  - (эндпоинта удаления нет)
 *
 * Изоляция разговоров между пользователями и привязка к товарам — на нашей стороне,
 * через [AiConversationRepository] (маппинг conversationId == chatId).
 */
@Service
class AiBotService(
    @param:Qualifier("aiAgentWebClient") private val webClient: WebClient,
    private val conversationRepository: AiConversationRepository,
    private val properties: AiAgentProperties,
    private val objectMapper: ObjectMapper
) {
    /**
     * Гарантировать существование чат-сессии в боте (создать, если нет).
     *
     * Бот идемпотентен по chatId + agentType, поэтому метод безопасно вызывать всегда —
     * это защищает от рассинхрона, когда локальный маппинг есть, а сессия в боте потерялась.
     *
     * @param chatId Идентификатор сессии (== conversationId)
     * @param topic Контекст/ограничение темы (название и описание товара); уходит в системный промпт
     * @throws AiBotServiceException при ошибке связи с ботом
     */
    suspend fun ensureConversation(chatId: String, topic: String?, description: String? = null) =
        ensureConversationWithAgent(properties.agentType, chatId, topic, description)

    /**
     * Гарантировать существование сессии дизайнера флорариумов (agentType=florarium).
     *
     * Отличается от [ensureConversation] только типом агента — бот выбирает системный промпт,
     * умеющий генерировать картинки флорариумов. Используется чатом на /custom-terrarium.
     */
    suspend fun ensureFlorariumConversation(chatId: String, topic: String?) =
        ensureConversationWithAgent(properties.florariumAgentType, chatId, topic)

    /**
     * Гарантировать существование сессии эксперта-мастера (agentType=florarium-expert).
     *
     * Для этого агента [topic] — JSON-строка {"sourceChatId":"...","imageUrl":"..."} (бот по ней
     * подтягивает историю исходного разговора-дизайнера и видит картинку заказа). Используется в админке.
     */
    suspend fun ensureFlorariumExpertConversation(chatId: String, topic: String?) =
        ensureConversationWithAgent(properties.florariumExpertAgentType, chatId, topic)

    private suspend fun ensureConversationWithAgent(agentType: String, chatId: String, topic: String?, description: String? = null) {
        logger.debug("Ensuring chat session {} (agentType={})", chatId, agentType)

        try {
            webClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(CreateChatRequest(agentType, chatId, topic, description))
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .awaitBodilessEntity()
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI bot", e)
            throw AiBotServiceException("Failed to connect to AI bot", e)
        } catch (e: TimeoutException) {
            logger.error("AI bot request timed out", e)
            throw AiBotTimeoutException("AI bot request timed out", e)
        }
    }

    /**
     * Отправить сообщение и получить ответ потоком (SSE-токены).
     *
     * Валидация владения выполняется до возврата (cold) Flow, поэтому чужой пользователь
     * не сможет начать стрим. Ошибки апстрима гасятся в поток вежливым сообщением,
     * чтобы фронт получил корректное завершение SSE, а не разрыв соединения.
     *
     * @return холодный поток фрагментов ответа (токенов)
     */
    suspend fun streamMessage(userId: String, conversationId: String, message: String): Flow<String> {
        logger.debug("Streaming message to conversation {} for user {}", conversationId, userId)
        validateOwnership(userId, conversationId)
        return rawTextStream(conversationId, message)
    }

    /**
     * Текстовый стрим эксперта-мастера (admin). Авторизация — по роли ADMIN на уровне контроллера,
     * а conversationId выводится из доверенного id заказа, поэтому проверка владения здесь не нужна.
     */
    fun streamFlorariumExpertMessage(conversationId: String, message: String): Flow<String> {
        logger.debug("Streaming florarium-expert message to conversation {}", conversationId)
        return rawTextStream(conversationId, message)
    }

    /**
     * Сырой текстовый SSE-стрим бота (/{chatId}/chat/stream) без проверки владения.
     *
     * Читаем поток как сырые байты, а не через bodyToFlow<String>(): стандартный SSE-ридер Spring
     * срезает один пробел после "data:" (спецификация SSE), что съедает значимые ведущие пробелы
     * токенов LLM (" любит" -> "любит") и склеивает слова. Парсим SSE сами и берём данные дословно.
     */
    private fun rawTextStream(conversationId: String, message: String): Flow<String> {
        val rawChunks = webClient.post()
            .uri("/{chatId}/chat/stream", conversationId)
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(message)
            .retrieve()
            .bodyToFlow<DataBuffer>()

        return sseDataFlow(rawChunks)
            .catch { e ->
                logger.error("Error while streaming AI response for conversation {}", conversationId, e)
                emit("\n\nИзвините, не удалось получить ответ из-за временного сбоя связи. Пожалуйста, повторите запрос.")
            }
    }

    /**
     * Стриминг ответа дизайнера флорариумов с генерацией картинок (эндпоинт бота stream-images).
     *
     * В отличие от [streamMessage], каждое SSE-событие бота — это JSON [BotStreamEvent]
     * (текст ИЛИ ссылка на картинку). Переиспользуем тот же байтовый SSE-парсер [sseDataFlow]
     * (он отдаёт сырую data-строку = JSON), затем десериализуем и нормализуем в [FlorariumChunk]
     * для фронта: ссылки картинок переписываются на backend-прокси Florics.
     *
     * @return холодный поток [FlorariumChunk] (текст и/или картинки)
     */
    suspend fun streamFlorariumMessage(userId: String, conversationId: String, message: String): Flow<FlorariumChunk> {
        logger.debug("Streaming florarium message to conversation {} for user {}", conversationId, userId)
        validateOwnership(userId, conversationId)

        val rawChunks = webClient.post()
            .uri("/{chatId}/chat/stream-images", conversationId)
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(message)
            .retrieve()
            .bodyToFlow<DataBuffer>()

        return sseDataFlow(rawChunks)
            .mapNotNull { parseBotEvent(it) }
            .catch { e ->
                logger.error("Error while streaming florarium response for conversation {}", conversationId, e)
                emit(FlorariumChunk(type = "text", text = "\n\nИзвините, не удалось получить ответ из-за временного сбоя связи. Пожалуйста, повторите запрос."))
            }
    }

    /**
     * Десериализовать JSON-событие бота в [FlorariumChunk]. Нераспарсенные/неизвестные события
     * пропускаются (null), чтобы один битый чанк не рвал весь стрим.
     */
    private fun parseBotEvent(json: String): FlorariumChunk? {
        val event = try {
            objectMapper.readValue(json, BotStreamEvent::class.java)
        } catch (e: Exception) {
            logger.warn("Skipping unparseable florarium SSE event: {}", json)
            return null
        }
        return when (event.type) {
            "text" -> event.text?.let { FlorariumChunk(type = "text", text = it) }
            "image" -> event.url?.let { FlorariumChunk(type = "image", imageUrl = rewriteImageUrl(it)) }
            else -> null
        }
    }

    /** Переписать относительный URL картинки бота (/api/v1/ollama/images/<id>) на путь backend-прокси Florics. */
    private fun rewriteImageUrl(botUrl: String): String =
        "/api/aibot/florarium/images/" + botUrl.substringAfterLast('/')

    /**
     * Прокси байтов сгенерированной картинки от бота (GET /images/{id}).
     *
     * Используем retrieve().toEntityFlux(): тело стримится вниз лениво (без буферизации PNG в память)
     * и его жизненный цикл управляется корректно. Заголовки апстрима (включая Content-Type)
     * сохраняются в ResponseEntity. ВАЖНО: нельзя возвращать тело наружу из exchangeToMono —
     * соединение освободится до подписки writer'а и тело придёт пустым.
     */
    fun proxyImage(imageId: String): Mono<ResponseEntity<Flux<DataBuffer>>> =
        webClient.get()
            .uri("/images/{id}", imageId)
            .accept(MediaType.ALL)
            .retrieve()
            .toEntityFlux(DataBuffer::class.java)

    /**
     * Ручной разбор SSE-потока бота с сохранением ведущих пробелов в data-полях.
     *
     * Граница событий ищется на уровне байтов (`\n` = 0x0A — ASCII, никогда не является частью
     * многобайтовой UTF-8 последовательности), поэтому двухбайтовая кириллица не рвётся между чанками.
     * Полный байтовый блок события декодируется как UTF-8 целиком.
     */
    private fun sseDataFlow(chunks: Flow<DataBuffer>): Flow<String> = flow {
        var buf = ByteArray(0)
        chunks.collect { dataBuffer ->
            val incoming = ByteArray(dataBuffer.readableByteCount())
            dataBuffer.read(incoming)
            DataBufferUtils.release(dataBuffer)
            // \r (0x0D) выкидываем на уровне фрейминга — он ASCII и не часть UTF-8 символа
            buf += incoming.filter { it != CR }.toByteArray()

            var sep = indexOfDoubleLf(buf)
            while (sep >= 0) {
                val eventBytes = buf.copyOfRange(0, sep)
                buf = buf.copyOfRange(sep + 2, buf.size)
                extractSseData(String(eventBytes, Charsets.UTF_8))?.let { if (it.isNotEmpty()) emit(it) }
                sep = indexOfDoubleLf(buf)
            }
        }
        // Хвост без завершающего разделителя
        if (buf.isNotEmpty()) {
            extractSseData(String(buf, Charsets.UTF_8))?.let { if (it.isNotEmpty()) emit(it) }
        }
    }

    /** Индекс первого `\n\n` в массиве байтов или -1. */
    private fun indexOfDoubleLf(arr: ByteArray): Int {
        for (i in 0 until arr.size - 1) {
            if (arr[i] == LF && arr[i + 1] == LF) return i
        }
        return -1
    }

    /**
     * Достать payload из блока SSE-события: берём всё после "data:" ДОСЛОВНО (без срезания пробела),
     * несколько data-строк склеиваем через "\n". Не-data строки (комментарии, event/id) игнорируем.
     */
    private fun extractSseData(event: String): String? {
        val dataLines = event.split("\n").filter { it.startsWith("data:") }
        if (dataLines.isEmpty()) return null
        return dataLines.joinToString("\n") { it.substring(5) }
    }

    /**
     * Отправить сообщение и получить ответ целиком (нестримовый fallback).
     *
     * @return синтезированный [ChatResponse] (бот возвращает строку)
     */
    suspend fun sendMessage(userId: String, conversationId: String, message: String): ChatResponse {
        logger.debug("Sending message to conversation {} for user {}", conversationId, userId)
        validateOwnership(userId, conversationId)

        val text = try {
            webClient.post()
                .uri("/{chatId}/chat", conversationId)
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .bodyValue(message)
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .awaitBody<String>()
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI bot", e)
            throw AiBotServiceException("Failed to connect to AI bot", e)
        } catch (e: TimeoutException) {
            logger.error("AI bot request timed out", e)
            throw AiBotTimeoutException("AI bot request timed out", e)
        }

        return ChatResponse(
            conversationId = UUID.fromString(conversationId),
            messageId = UUID.randomUUID(),
            response = text,
            timestamp = OffsetDateTime.now()
        ).also { logger.debug("AI response for conversation {}: {} chars", conversationId, it.response.length) }
    }

    /**
     * Анализ фотографий растения ботом (agentType=photo-analyzer).
     *
     * Картинки кодируются в data-URL и уходят боту, текст сообщения пуст.
     * Возвращает PlantCard для автозаполнения карточки товара. См. [callPhotoAnalyzer].
     */
    suspend fun analyzePhotos(images: List<ImageBytes>): PlantCardDto {
        val dataUrls = images.map {
            "data:${it.contentType};base64," + Base64.getEncoder().encodeToString(it.bytes)
        }
        return callPhotoAnalyzer(message = "", dataUrls = dataUrls)
    }

    /**
     * Анализ растения по названию тем же ботом (agentType=photo-analyzer), без изображений.
     *
     * Боту уходит текст названия в message, список картинок пуст. Возвращает ту же PlantCard,
     * что и [analyzePhotos], — для автозаполнения карточки товара по введённому названию.
     */
    suspend fun analyzeByName(name: String): PlantCardDto =
        callPhotoAnalyzer(message = name, dataUrls = emptyList())

    /**
     * Одноразовый вызов агента photo-analyzer.
     *
     * Генерируем chatId, создаём сессию, отправляем {message, images} на /chat/stream-with-images,
     * читаем единственное SSE-событие с JSON PlantCard. Маппинг разговора в БД не ведём.
     * JSON парсим через Map (ключи snake_case), чтобы не зависеть от аннотаций Jackson.
     */
    private suspend fun callPhotoAnalyzer(message: String, dataUrls: List<String>): PlantCardDto {
        val chatId = UUID.randomUUID().toString()
        ensureConversationWithAgent(properties.photoAnalyzerAgentType, chatId, null)

        val json = try {
            val rawChunks = webClient.post()
                .uri("/{chatId}/chat/stream-with-images", chatId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(mapOf("message" to message, "images" to dataUrls))
                .retrieve()
                .bodyToFlow<DataBuffer>()
            sseDataFlow(rawChunks).toList().joinToString("")
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI bot (photo-analyzer)", e)
            throw AiBotServiceException("Failed to connect to AI bot", e)
        } catch (e: TimeoutException) {
            logger.error("AI bot photo-analyzer timed out", e)
            throw AiBotTimeoutException("AI bot request timed out", e)
        }

        if (json.isBlank()) throw AiBotServiceException("Empty photo-analyzer response")
        return parsePlantCard(json)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePlantCard(json: String): PlantCardDto {
        val map = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
        return PlantCardDto(
            name = map["name"] as? String,
            shortDescription = map["short_description"] as? String,
            fullDescription = map["full_description"] as? String,
            care = map["care"] as? String,
            error = map["error"] as? String
        )
    }

    /**
     * Получить историю сообщений разговора.
     *
     * Бот возвращает всю историю по chatId. Если сессия ещё не создана (404) — пустой список.
     * Параметр [limit] оставлен для совместимости контракта /api/aibot; бот лимит не поддерживает.
     */
    suspend fun getMessages(userId: String, conversationId: String, limit: Int = 50): List<MessageResponse> {
        logger.debug("Getting messages for conversation {} (user {})", conversationId, userId)
        validateOwnership(userId, conversationId)
        return fetchHistory(conversationId)
    }

    /**
     * История экспертного разговора (admin). Без проверки владения — см. [streamFlorariumExpertMessage].
     */
    suspend fun getExpertMessages(conversationId: String): List<MessageResponse> {
        logger.debug("Getting florarium-expert messages for conversation {}", conversationId)
        return fetchHistory(conversationId)
    }

    /**
     * Все URL картинок разговора (по порядку, без дублей). Без проверки владения — для админки,
     * чтобы показать мастеру карусель картинок исходного разговора-дизайнера.
     */
    suspend fun getConversationImages(conversationId: String): List<String> =
        fetchHistory(conversationId).flatMap { it.imageUrls }.distinct()

    /**
     * Загрузить историю сообщений разговора из бота без проверки владения.
     * Если сессии ещё нет (404) — пустой список.
     */
    private suspend fun fetchHistory(conversationId: String): List<MessageResponse> {
        return try {
            webClient.get()
                .uri("/{chatId}/chat", conversationId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlow<OllamaHistoryMessage>()
                .map { it.toMessageResponse(conversationId) }
                .toList()
        } catch (e: WebClientResponseException.NotFound) {
            logger.debug("No session in bot for conversation {} yet — returning empty history", conversationId)
            emptyList()
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI bot", e)
            throw AiBotServiceException("Failed to connect to AI bot", e)
        } catch (e: TimeoutException) {
            logger.error("AI bot request timed out", e)
            throw AiBotTimeoutException("AI bot request timed out", e)
        }
    }

    /**
     * «Удалить» разговор. У бота нет эндпоинта удаления, поэтому чистим только локальный маппинг.
     * Фронт после этого создаёт новый chatId — это даёт пользователю свежий чат.
     */
    suspend fun deleteConversation(userId: String, conversationId: String) {
        logger.debug("Deleting conversation mapping {} for user {}", conversationId, userId)
        validateOwnership(userId, conversationId)
        conversationRepository.deleteByConversationId(conversationId)
            .also { logger.debug("Conversation mapping {} deleted", conversationId) }
    }

    /**
     * Валидация владения разговором (изоляция между пользователями).
     */
    private suspend fun validateOwnership(userId: String, conversationId: String) {
        val ownerId = conversationRepository.findByConversationId(conversationId)
            ?: throw ConversationNotFoundException(conversationId)

        if (ownerId != userId) {
            logger.warn("User {} attempted to access conversation {} owned by user {}", userId, conversationId, ownerId)
            throw ConversationAccessDeniedException(conversationId)
        }
    }

    private fun OllamaHistoryMessage.toMessageResponse(conversationId: String): MessageResponse =
        MessageResponse(
            id = id,
            conversationId = UUID.fromString(conversationId),
            role = runCatching { MessageRole.valueOf(role.uppercase()) }.getOrDefault(MessageRole.ASSISTANT),
            content = content,
            createdAt = createdAt,
            // Картинки бота недоступны браузеру напрямую — переписываем на backend-прокси (как в стриме)
            imageUrls = imageUrls.map { rewriteImageUrl(it) }
        )

    private companion object {
        private const val LF: Byte = 0x0A  // '\n'
        private const val CR: Byte = 0x0D  // '\r'

        /** Error handler для 4xx ошибок бота */
        private val errorHandler4xx: (ClientResponse) -> Mono<out Throwable> = { response ->
            response.bodyToMono<String>().map { body ->
                when (response.statusCode().value()) {
                    404 -> AiBotServiceException("Resource not found in AI bot")
                    400 -> AiBotServiceException("Bad request to AI bot: $body")
                    else -> AiBotServiceException("AI bot client error (${response.statusCode()}): $body")
                }
            }.defaultIfEmpty(AiBotServiceException("AI bot client error: ${response.statusCode()}"))
        }

        /** Error handler для 5xx ошибок бота */
        private val errorHandler5xx: (ClientResponse) -> Mono<out Throwable> = { response ->
            response.bodyToMono<String>().map { body ->
                AiBotServiceException("AI bot server error (${response.statusCode()}): $body")
            }.defaultIfEmpty(AiBotServiceException("AI bot server error: ${response.statusCode()}"))
        }

        private val logger = LoggerFactory.getLogger(AiBotService::class.java)
    }
}
