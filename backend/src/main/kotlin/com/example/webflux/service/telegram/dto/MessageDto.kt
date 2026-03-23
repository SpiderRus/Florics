package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект сообщения Telegram
 * https://core.telegram.org/bots/api#message
 */
@Schema(description = "Сообщение Telegram")
data class MessageDto(
    @Schema(description = "Уникальный идентификатор сообщения в данном чате")
    @JsonProperty("message_id")
    val messageId: Long,

    @Schema(description = "Уникальный идентификатор ветки сообщений (топика), к которой принадлежит сообщение")
    @JsonProperty("message_thread_id")
    val messageThreadId: Long? = null,

    @Schema(description = "Отправитель сообщения")
    @JsonProperty("from")
    val from: TelegramUserDto? = null,

    @Schema(description = "Отправитель сообщения, отправленного от имени чата")
    @JsonProperty("sender_chat")
    val senderChat: ChatDto? = null,

    @Schema(description = "Дата отправки сообщения в Unix time")
    @JsonProperty("date")
    val date: Long,

    @Schema(description = "Чат, к которому принадлежит сообщение")
    @JsonProperty("chat")
    val chat: ChatDto,

    @Schema(description = "Для пересланных сообщений - отправитель оригинального сообщения")
    @JsonProperty("forward_from")
    val forwardFrom: TelegramUserDto? = null,

    @Schema(description = "Для пересланных сообщений - дата отправки оригинального сообщения в Unix time")
    @JsonProperty("forward_date")
    val forwardDate: Long? = null,

    @Schema(description = "Для ответов - оригинальное сообщение")
    @JsonProperty("reply_to_message")
    val replyToMessage: MessageDto? = null,

    @Schema(description = "Дата последнего редактирования сообщения в Unix time")
    @JsonProperty("edit_date")
    val editDate: Long? = null,

    @Schema(description = "Для текстовых сообщений - фактический текст сообщения в UTF-8")
    @JsonProperty("text")
    val text: String? = null,

    @Schema(description = "Для сообщений с подписью - текст подписи")
    @JsonProperty("caption")
    val caption: String? = null,

    @Schema(description = "Сообщение является фото, доступные размеры фотографии")
    @JsonProperty("photo")
    val photo: List<PhotoSizeDto>? = null,

    @Schema(description = "Сообщение является файлом, информация о файле")
    @JsonProperty("document")
    val document: DocumentDto? = null,

    @Schema(description = "Встроенная клавиатура, прикрепленная к сообщению")
    @JsonProperty("reply_markup")
    val replyMarkup: InlineKeyboardMarkupDto? = null
)
