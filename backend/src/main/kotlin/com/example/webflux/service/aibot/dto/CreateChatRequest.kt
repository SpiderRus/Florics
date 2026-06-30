package com.example.webflux.service.aibot.dto

/**
 * DTO для создания чат-сессии в боте OllamaTestController (AIAgentNew).
 *
 * Отправляется на POST /api/v1/ollama/chat. Сессия идемпотентна по [chatId] + [agentType].
 *
 * @property agentType Тип агента (например "plants") — выбирает системный промпт бота
 * @property chatId Идентификатор сессии, задаётся клиентом (используем UUID = conversation_id)
 * @property topic Короткое ограничение темы (название товара); уходит в системный промпт И в RAG-компрессию
 * @property description Свободный контекст (категория, описание, призыв к покупке); подмешивается в промпт, но НЕ в RAG-компрессию
 */
data class CreateChatRequest(
    val agentType: String,
    val chatId: String,
    val topic: String? = null,
    val description: String? = null
)
