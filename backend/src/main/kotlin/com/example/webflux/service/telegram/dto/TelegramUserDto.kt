package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект пользователя Telegram
 * https://core.telegram.org/bots/api#user
 */
@Schema(description = "Пользователь Telegram")
data class TelegramUserDto(
    @Schema(description = "Уникальный идентификатор пользователя или бота")
    @JsonProperty("id")
    val id: Long,

    @Schema(description = "True, если это бот")
    @JsonProperty("is_bot")
    val isBot: Boolean,

    @Schema(description = "Имя пользователя или бота")
    @JsonProperty("first_name")
    val firstName: String,

    @Schema(description = "Фамилия пользователя или бота")
    @JsonProperty("last_name")
    val lastName: String? = null,

    @Schema(description = "Имя пользователя (username) пользователя или бота")
    @JsonProperty("username")
    val username: String? = null,

    @Schema(description = "IETF тег языка пользователя")
    @JsonProperty("language_code")
    val languageCode: String? = null,

    @Schema(description = "True, если пользователь имеет подписку Telegram Premium")
    @JsonProperty("is_premium")
    val isPremium: Boolean? = null,

    @Schema(description = "True, если пользователь добавил бота в меню вложений")
    @JsonProperty("added_to_attachment_menu")
    val addedToAttachmentMenu: Boolean? = null
)
