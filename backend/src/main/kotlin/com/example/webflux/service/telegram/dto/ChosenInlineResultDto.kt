package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект выбранного inline-результата Telegram
 * https://core.telegram.org/bots/api#choseninlineresult
 */
@Schema(description = "Выбранный inline-результат Telegram")
data class ChosenInlineResultDto(
    @Schema(description = "Уникальный идентификатор выбранного результата")
    @JsonProperty("result_id")
    val resultId: String,

    @Schema(description = "Пользователь, выбравший результат")
    @JsonProperty("from")
    val from: TelegramUserDto,

    @Schema(description = "Местоположение отправителя, только для ботов, требующих геолокацию пользователя")
    @JsonProperty("location")
    val location: LocationDto? = null,

    @Schema(description = "Идентификатор отправленного inline-сообщения")
    @JsonProperty("inline_message_id")
    val inlineMessageId: String? = null,

    @Schema(description = "Запрос, который был использован для получения результата")
    @JsonProperty("query")
    val query: String
)
