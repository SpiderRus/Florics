package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на аутентификацию")
data class AuthRequest(
    @JsonProperty("email")
    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @JsonProperty("password")
    @Schema(description = "Пароль", example = "password123")
    val password: String
)
