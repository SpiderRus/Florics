package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ при успешной аутентификации")
data class AuthResponse(
    @Schema(description = "Opaque токен доступа")
    val accessToken: String,

    @Schema(description = "Тип токена", example = "Bearer")
    val tokenType: String = "Bearer",

    @Schema(description = "Время жизни токена в секундах", example = "86400")
    val expiresIn: Long,

    @Schema(description = "Данные пользователя")
    val user: UserDto
)

@Schema(description = "Данные пользователя (без пароля)")
data class UserDto(
    @Schema(description = "ID пользователя")
    val id: Long,

    @Schema(description = "Имя пользователя")
    val name: String,

    @Schema(description = "Email пользователя")
    val email: String,

    @Schema(description = "Роли пользователя")
    val roles: Set<String>
)
