package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на аутентификацию")
data class AuthRequest(
    @field:Email(message = "Некорректный формат email")
    @field:NotBlank(message = "Email обязателен")
    @JsonProperty("email")
    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @field:Size(min = 8, max = 128, message = "Пароль должен содержать от 8 до 128 символов")
    @field:NotBlank(message = "Пароль обязателен")
    @JsonProperty("password")
    @Schema(description = "Пароль", example = "Password123!")
    val password: String
)
