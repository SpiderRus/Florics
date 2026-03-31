package com.example.webflux.controller.model

import com.example.webflux.security.ValidPassword
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на регистрацию")
data class RegisterRequest(
    @field:Email(message = "Некорректный формат email")
    @field:NotBlank(message = "Email обязателен")
    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @field:Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
    @field:NotBlank(message = "Имя обязательно")
    @Schema(description = "Имя пользователя", example = "Иван Иванов")
    val name: String,

    @field:ValidPassword
    @field:NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль (минимум 8 символов, заглавная буква, строчная буква, цифра, спецсимвол)", example = "Password123!")
    val password: String
)
