package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на обновление пользователя")
data class UpdateUserRequest(
    @Schema(description = "Новое имя пользователя", example = "Иван Иванов")
    val name: String? = null,

    @Schema(description = "Новый email пользователя", example = "newemail@example.com")
    val email: String? = null,

    @Schema(description = "Новый пароль пользователя", example = "newpassword123")
    val password: String? = null,

    @Schema(description = "Новые роли пользователя", example = "[\"USER\", \"ADMIN\"]")
    val roles: Set<String>? = null
)
