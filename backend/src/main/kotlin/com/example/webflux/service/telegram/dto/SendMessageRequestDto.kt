package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект запроса для отправки сообщений
 * https://core.telegram.org/bots/api#sendmessage
 */
@Schema(description = "Запрос для отправки сообщения")
data class SendMessageRequestDto(
    @Schema(description = "Уникальный идентификатор целевого чата или имя пользователя (username)")
    @JsonProperty("chat_id")
    val chatId: String,

    @Schema(description = "Уникальный идентификатор целевой ветки сообщений (топика)")
    @JsonProperty("message_thread_id")
    val messageThreadId: Long? = null,

    @Schema(description = "Текст отправляемого сообщения")
    @JsonProperty("text")
    val text: String,

    @Schema(description = "Режим разбора сущностей в тексте сообщения")
    @JsonProperty("parse_mode")
    val parseMode: String? = null,

    @Schema(description = "Отключает предварительный просмотр ссылок в данном сообщении")
    @JsonProperty("disable_web_page_preview")
    val disableWebPagePreview: Boolean? = null,

    @Schema(description = "Отправляет сообщение без звука")
    @JsonProperty("disable_notification")
    val disableNotification: Boolean? = null,

    @Schema(description = "Защищает содержимое отправленного сообщения от пересылки и сохранения")
    @JsonProperty("protect_content")
    val protectContent: Boolean? = null,

    @Schema(description = "Если сообщение является ответом, ID оригинального сообщения")
    @JsonProperty("reply_to_message_id")
    val replyToMessageId: Long? = null,

    @Schema(description = "Передайте True, если сообщение должно быть отправлено, даже если указанное сообщение для ответа не найдено")
    @JsonProperty("allow_sending_without_reply")
    val allowSendingWithoutReply: Boolean? = null,

    @Schema(description = "Дополнительные опции интерфейса")
    @JsonProperty("reply_markup")
    val replyMarkup: Any? = null // Может быть InlineKeyboardMarkup, ReplyKeyboardMarkup и т.д.
)
