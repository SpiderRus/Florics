package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект пользовательской клавиатуры Telegram
 * https://core.telegram.org/bots/api#replykeyboardmarkup
 */
@Schema(description = "Пользовательская клавиатура Telegram")
data class ReplyKeyboardMarkupDto(
    @Schema(description = "Массив рядов кнопок, каждый из которых представлен массивом объектов KeyboardButton")
    @JsonProperty("keyboard")
    val keyboard: List<List<KeyboardButtonDto>>,

    @Schema(description = "Запрашивает у клиентов всегда показывать клавиатуру, когда обычная клавиатура скрыта")
    @JsonProperty("is_persistent")
    val isPersistent: Boolean? = null,

    @Schema(description = "Запрашивает у клиентов изменить размер клавиатуры по вертикали для оптимального соответствия")
    @JsonProperty("resize_keyboard")
    val resizeKeyboard: Boolean? = null,

    @Schema(description = "Запрашивает у клиентов скрыть клавиатуру сразу после использования")
    @JsonProperty("one_time_keyboard")
    val oneTimeKeyboard: Boolean? = null,

    @Schema(description = "Текст-заполнитель, который будет показан в поле ввода, когда клавиатура активна")
    @JsonProperty("input_field_placeholder")
    val inputFieldPlaceholder: String? = null,

    @Schema(description = "Используйте этот параметр, если хотите показать клавиатуру только определенным пользователям")
    @JsonProperty("selective")
    val selective: Boolean? = null
)
