package com.example.webflux.service.aibot.dto

/**
 * DTO для запроса создания нового разговора
 *
 * @property title Название разговора
 */
data class ConversationCreateRequest(
    val title: String
)
