package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Данные пользователя (без пароля)")
data class UserResponseDto(
    @Schema(description = "ID пользователя", example = "1")
    val id: Long,

    @Schema(description = "Имя пользователя", example = "Иван Иванов")
    val name: String,

    @Schema(description = "Email пользователя", example = "ivan@example.com")
    val email: String,

    @Schema(description = "Роли пользователя", example = "[\"USER\", \"BUYER\"]")
    val roles: Set<String>
)
