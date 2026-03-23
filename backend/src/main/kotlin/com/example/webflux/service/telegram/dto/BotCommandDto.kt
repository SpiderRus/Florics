package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект команды бота Telegram
 * https://core.telegram.org/bots/api#botcommand
 */
@Schema(description = "Команда бота Telegram")
data class BotCommandDto(
    @Schema(description = "Текст команды; 1-32 символа")
    @JsonProperty("command")
    val command: String,

    @Schema(description = "Описание команды; 1-256 символов")
    @JsonProperty("description")
    val description: String
)
