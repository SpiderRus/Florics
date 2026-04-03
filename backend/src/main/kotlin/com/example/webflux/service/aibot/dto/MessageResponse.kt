package com.example.webflux.service.aibot.dto

import java.time.OffsetDateTime
import java.util.*

/**
 * DTO для представления сообщения в ответах
 *
 * @property id Уникальный идентификатор сообщения
 * @property conversationId ID разговора, к которому относится сообщение
 * @property role Роль отправителя (USER/ASSISTANT/SYSTEM)
 * @property content Текстовое содержимое сообщения
 * @property createdAt Время создания сообщения
 */
data class MessageResponse(
    val id: UUID,
    val conversationId: UUID,
    val role: MessageRole,
    val content: String,
    val createdAt: OffsetDateTime
)
