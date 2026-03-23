package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект кнопки встроенной клавиатуры Telegram
 * https://core.telegram.org/bots/api#inlinekeyboardbutton
 */
@Schema(description = "Кнопка встроенной клавиатуры Telegram")
data class InlineKeyboardButtonDto(
    @Schema(description = "Текст на кнопке")
    @JsonProperty("text")
    val text: String,

    @Schema(description = "HTTP или tg:// URL, который будет открыт при нажатии кнопки")
    @JsonProperty("url")
    val url: String? = null,

    @Schema(description = "Данные, которые будут отправлены в callback-запросе боту при нажатии кнопки")
    @JsonProperty("callback_data")
    val callbackData: String? = null,

    @Schema(description = "Если установлено, при нажатии кнопки пользователю будет предложено выбрать один из своих чатов")
    @JsonProperty("switch_inline_query")
    val switchInlineQuery: String? = null,

    @Schema(description = "Если установлено, при нажатии кнопки будет вставлено имя пользователя бота и указанный inline-запрос")
    @JsonProperty("switch_inline_query_current_chat")
    val switchInlineQueryCurrentChat: String? = null,

    @Schema(description = "Описание Web-приложения, которое будет запущено при нажатии кнопки")
    @JsonProperty("web_app")
    val webApp: WebAppInfoDto? = null,

    @Schema(description = "HTTPS URL, используемый для автоматической авторизации пользователя")
    @JsonProperty("login_url")
    val loginUrl: LoginUrlDto? = null,

    @Schema(description = "Если установлено, при нажатии кнопки будет запущен опрос")
    @JsonProperty("callback_game")
    val callbackGame: Any? = null,

    @Schema(description = "Если True, текущий пользователь сможет оплатить товары")
    @JsonProperty("pay")
    val pay: Boolean? = null
)
