package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект кнопки клавиатуры Telegram
 * https://core.telegram.org/bots/api#keyboardbutton
 */
@Schema(description = "Кнопка клавиатуры Telegram")
data class KeyboardButtonDto(
    @Schema(description = "Текст на кнопке")
    @JsonProperty("text")
    val text: String,

    @Schema(description = "Если True, номер телефона пользователя будет отправлен как контакт")
    @JsonProperty("request_contact")
    val requestContact: Boolean? = null,

    @Schema(description = "Если True, текущее местоположение пользователя будет отправлено")
    @JsonProperty("request_location")
    val requestLocation: Boolean? = null,

    @Schema(description = "Если указано, пользователю будет предложено создать опрос")
    @JsonProperty("request_poll")
    val requestPoll: KeyboardButtonPollTypeDto? = null,

    @Schema(description = "Если указано, будет запущено описанное Web-приложение")
    @JsonProperty("web_app")
    val webApp: WebAppInfoDto? = null
)
