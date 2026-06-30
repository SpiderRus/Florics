package com.example.webflux.service.aibot.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Событие SSE-потока бота OllamaTestController.chatStreamImages() (AIAgentNew).
 *
 * Приходит как JSON в поле `data` каждого SSE-события и бывает двух видов (различаются по [type]):
 *  - текст:    {"type":"text","text":"<фрагмент>"}
 *  - картинка: {"type":"image","url":"/api/v1/ollama/images/<uuid>","mimeType":"image/png"}
 *
 * Картинка передаётся ссылкой (не base64); байты тянутся отдельным GET по [url].
 * Неизвестные поля игнорируются — контракт бота может расширяться.
 *
 * @property type Тип события: "text" | "image"
 * @property text Фрагмент текста (для type=text)
 * @property url Относительный URL картинки на стороне бота (для type=image)
 * @property mimeType MIME-тип картинки (для type=image)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BotStreamEvent(
    val type: String,
    val text: String? = null,
    val url: String? = null,
    val mimeType: String? = null
)
