package com.example.webflux.service.aibot.dto

/**
 * DTO для запроса отправки сообщения в чат
 *
 * @property message Текст сообщения от пользователя (обязательное, не может быть пустым)
 * @property useRag Флаг включения RAG (Retrieval-Augmented Generation).
 *                  true = выполнить векторный поиск и дополнить промпт контекстом
 *                  false = использовать только базовые знания LLM
 *                  По умолчанию: true
 */
data class ChatRequest(
    val message: String,
    val useRag: Boolean = true
)
