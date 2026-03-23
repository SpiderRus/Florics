package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект URL для авторизации Telegram
 * https://core.telegram.org/bots/api#loginurl
 */
@Schema(description = "URL для авторизации Telegram")
data class LoginUrlDto(
    @Schema(description = "HTTPS URL, который будет открыт с данными авторизации пользователя")
    @JsonProperty("url")
    val url: String,

    @Schema(description = "Новый текст кнопки в пересланных сообщениях")
    @JsonProperty("forward_text")
    val forwardText: String? = null,

    @Schema(description = "Имя пользователя (username) бота, который будет использоваться для авторизации пользователя")
    @JsonProperty("bot_username")
    val botUsername: String? = null,

    @Schema(description = "Передайте True, чтобы запросить разрешение для вашего бота отправлять сообщения пользователю")
    @JsonProperty("request_write_access")
    val requestWriteAccess: Boolean? = null
)
