package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект callback-запроса Telegram
 * https://core.telegram.org/bots/api#callbackquery
 */
@Schema(description = "Callback-запрос Telegram")
data class CallbackQueryDto(
    @Schema(description = "Уникальный идентификатор запроса")
    @JsonProperty("id")
    val id: String,

    @Schema(description = "Отправитель")
    @JsonProperty("from")
    val from: TelegramUserDto,

    @Schema(description = "Сообщение с callback-кнопкой, которая инициировала запрос")
    @JsonProperty("message")
    val message: MessageDto? = null,

    @Schema(description = "Идентификатор сообщения, отправленного через бота в inline-режиме")
    @JsonProperty("inline_message_id")
    val inlineMessageId: String? = null,

    @Schema(description = "Глобальный идентификатор, однозначно соответствующий чату, в который было отправлено сообщение")
    @JsonProperty("chat_instance")
    val chatInstance: String,

    @Schema(description = "Данные, связанные с callback-кнопкой")
    @JsonProperty("data")
    val data: String? = null,

    @Schema(description = "Короткое имя игры, которое должно быть возвращено")
    @JsonProperty("game_short_name")
    val gameShortName: String? = null
)
