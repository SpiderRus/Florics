package com.example.webflux.service.aibot.dto

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

/**
 * DTO для представления разговора в ответах
 *
 * @property id Уникальный идентификатор разговора
 * @property title Название разговора
 * @property createdAt Дата и время создания
 * @property updatedAt Дата и время последнего обновления
 * @property messageCount Количество сообщений в разговоре
 */
data class ConversationResponse(
    val id: UUID,
    val title: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val messageCount: Int
)
