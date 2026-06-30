package com.example.webflux.service.aibot.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Элемент истории сообщений, который возвращает бот OllamaTestController
 * (GET /api/v1/ollama/{chatId}/chat).
 *
 * @property id Идентификатор сообщения
 * @property role Роль отправителя ("USER" / "ASSISTANT")
 * @property content Текст сообщения
 * @property createdAt Время создания
 * @property imageUrls Относительные URL картинок сообщения (/api/v1/ollama/images/<id>), для florarium-чата
 */
data class OllamaHistoryMessage(
    val id: UUID,
    val role: String,
    val content: String,
    val createdAt: OffsetDateTime,
    val imageUrls: List<String> = emptyList()
)
