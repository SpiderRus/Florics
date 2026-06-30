package com.example.webflux.service.aibot.dto

/**
 * Единица SSE-потока к фронтенду.
 *
 * Сериализуется в JSON (`{"t":"..."}`) внутри поля `data` SSE-события. JSON выбран намеренно:
 * токены LLM несут значимые ведущие пробелы (например " world"), а «голый» SSE срезает один
 * пробел после `data:` и склеивает слова. JSON сохраняет содержимое точно.
 *
 * @property t Фрагмент ответа (токен)
 */
data class TokenChunk(
    val t: String
)
