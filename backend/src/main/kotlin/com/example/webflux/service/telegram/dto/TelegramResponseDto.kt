package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Общий ответ API Telegram
 * https://core.telegram.org/bots/api#making-requests
 */
@Schema(description = "Ответ API Telegram")
data class TelegramResponseDto<T>(
    @Schema(description = "Успешно ли выполнен запрос")
    @JsonProperty("ok")
    val ok: Boolean,

    @Schema(description = "Результат запроса")
    @JsonProperty("result")
    val result: T? = null,

    @Schema(description = "Код ошибки")
    @JsonProperty("error_code")
    val errorCode: Int? = null,

    @Schema(description = "Описание результата или ошибки в читаемом формате")
    @JsonProperty("description")
    val description: String? = null
)
