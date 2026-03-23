package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект обновления Telegram
 * https://core.telegram.org/bots/api#update
 */
@Schema(description = "Обновление Telegram")
data class UpdateDto(
    @Schema(description = "Уникальный идентификатор обновления")
    @JsonProperty("update_id")
    val updateId: Long,

    @Schema(description = "Новое входящее сообщение любого типа")
    @JsonProperty("message")
    val message: MessageDto? = null,

    @Schema(description = "Новая версия известного боту сообщения, которое было отредактировано")
    @JsonProperty("edited_message")
    val editedMessage: MessageDto? = null,

    @Schema(description = "Новая запись в канале любого типа")
    @JsonProperty("channel_post")
    val channelPost: MessageDto? = null,

    @Schema(description = "Новая версия известной боту записи в канале, которая была отредактирована")
    @JsonProperty("edited_channel_post")
    val editedChannelPost: MessageDto? = null,

    @Schema(description = "Новый входящий callback-запрос")
    @JsonProperty("callback_query")
    val callbackQuery: CallbackQueryDto? = null,

    @Schema(description = "Новый входящий inline-запрос")
    @JsonProperty("inline_query")
    val inlineQuery: InlineQueryDto? = null,

    @Schema(description = "Результат inline-запроса, выбранный пользователем")
    @JsonProperty("chosen_inline_result")
    val chosenInlineResult: ChosenInlineResultDto? = null
)
