package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект типа опроса кнопки клавиатуры Telegram
 * https://core.telegram.org/bots/api#keyboardbuttonpolltype
 */
@Schema(description = "Тип опроса кнопки клавиатуры Telegram")
data class KeyboardButtonPollTypeDto(
    @Schema(description = "Если указан quiz, пользователю будет разрешено создавать только опросы в режиме викторины")
    @JsonProperty("type")
    val type: String? = null
)
