package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект встроенной клавиатуры Telegram
 * https://core.telegram.org/bots/api#inlinekeyboardmarkup
 */
@Schema(description = "Встроенная клавиатура Telegram")
data class InlineKeyboardMarkupDto(
    @Schema(description = "Массив рядов кнопок, каждый из которых представлен массивом объектов InlineKeyboardButton")
    @JsonProperty("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboardButtonDto>>
)
