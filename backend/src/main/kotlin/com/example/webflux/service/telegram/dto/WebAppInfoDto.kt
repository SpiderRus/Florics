package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект информации о Web-приложении Telegram
 * https://core.telegram.org/bots/api#webappinfo
 */
@Schema(description = "Информация о Web-приложении Telegram")
data class WebAppInfoDto(
    @Schema(description = "HTTPS URL Web-приложения, которое будет открыто с дополнительными данными")
    @JsonProperty("url")
    val url: String
)
