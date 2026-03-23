package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект inline-запроса Telegram
 * https://core.telegram.org/bots/api#inlinequery
 */
@Schema(description = "Inline-запрос Telegram")
data class InlineQueryDto(
    @Schema(description = "Уникальный идентификатор запроса")
    @JsonProperty("id")
    val id: String,

    @Schema(description = "Отправитель")
    @JsonProperty("from")
    val from: TelegramUserDto,

    @Schema(description = "Текст запроса (до 256 символов)")
    @JsonProperty("query")
    val query: String,

    @Schema(description = "Смещение результатов, которые должны быть возвращены")
    @JsonProperty("offset")
    val offset: String,

    @Schema(description = "Тип чата, из которого был отправлен inline-запрос")
    @JsonProperty("chat_type")
    val chatType: String? = null,

    @Schema(description = "Местоположение отправителя, только для ботов, запрашивающих геолокацию пользователя")
    @JsonProperty("location")
    val location: LocationDto? = null
)
