package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на создание пользователя")
data class CreateUserRequest(
    @Schema(description = "Имя пользователя", example = "Иван Иванов", required = true)
    val name: String,

    @Schema(description = "Email пользователя", example = "ivan@example.com", required = true)
    val email: String,

    @Schema(description = "Пароль пользователя", example = "password123", required = true)
    val password: String,

    @Schema(description = "Роли пользователя", example = "[\"USER\"]")
    val roles: Set<String> = setOf("USER")
)
