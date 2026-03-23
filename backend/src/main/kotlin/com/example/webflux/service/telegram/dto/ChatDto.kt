package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект чата Telegram
 * https://core.telegram.org/bots/api#chat
 */
@Schema(description = "Чат Telegram")
data class ChatDto(
    @Schema(description = "Уникальный идентификатор чата")
    @JsonProperty("id")
    val id: Long,

    @Schema(description = "Тип чата: private, group, supergroup или channel")
    @JsonProperty("type")
    val type: String,

    @Schema(description = "Название для супергрупп, каналов и групповых чатов")
    @JsonProperty("title")
    val title: String? = null,

    @Schema(description = "Имя пользователя (username) для личных чатов, супергрупп и каналов, если доступно")
    @JsonProperty("username")
    val username: String? = null,

    @Schema(description = "Имя собеседника в личном чате")
    @JsonProperty("first_name")
    val firstName: String? = null,

    @Schema(description = "Фамилия собеседника в личном чате")
    @JsonProperty("last_name")
    val lastName: String? = null,

    @Schema(description = "True, если супергруппа является форумом")
    @JsonProperty("is_forum")
    val isForum: Boolean? = null
)
