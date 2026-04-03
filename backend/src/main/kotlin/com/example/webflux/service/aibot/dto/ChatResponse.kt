package com.example.webflux.service.aibot.dto

import java.time.OffsetDateTime
import java.util.*

/**
 * DTO для ответа чата с RAG метаданными
 *
 * @property conversationId ID разговора
 * @property messageId ID созданного сообщения-ответа
 * @property response Текст ответа, сгенерированный LLM (Ollama llama3)
 * @property timestamp Время генерации ответа
 */
data class ChatResponse(
    val conversationId: UUID,
    val messageId: UUID,
    val response: String,
    val timestamp: OffsetDateTime
)
