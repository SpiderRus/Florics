package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на регистрацию")
data class RegisterRequest(
    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @Schema(description = "Имя пользователя", example = "Иван Иванов")
    val name: String,

    @Schema(description = "Пароль", example = "password123")
    val password: String
)
