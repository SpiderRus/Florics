package com.example.webflux.controller

import com.example.webflux.controller.model.*
import com.example.webflux.repository.UserRepository
import com.example.webflux.security.AuthenticationService
import com.example.webflux.security.SecurityUtils
import com.example.webflux.security.TokenInfo
import com.example.webflux.security.ValidPassword
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "API для регистрации и входа пользователей")
class AuthController(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository
) {

    @GetMapping("/test")
    @Operation(summary = "Тест", description = "Тестовый endpoint")
    suspend fun test(): ResponseEntity<String> {
        return ResponseEntity.ok("Auth controller works!")
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя по email и паролю")
    suspend fun login(@org.springframework.validation.annotation.Validated @RequestBody request: AuthRequest): ResponseEntity<AuthResponse> {
        val tokenInfo = authenticationService.authenticate(request.email, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(tokenInfo.toAuthResponse())
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Создание нового пользователя")
    suspend fun register(@org.springframework.validation.annotation.Validated @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        return try {
            val tokenInfo = authenticationService.register(
                request.email,
                request.name,
                request.password
            )
            ResponseEntity.status(HttpStatus.CREATED).body(tokenInfo.toAuthResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Выход", description = "Инвалидация токена")
    suspend fun logout(): ResponseEntity<Void> {
        val tokenInfo = SecurityUtils.getCurrentTokenInfo()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        authenticationService.revokeToken(tokenInfo.token)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Текущий пользователь", description = "Получить данные авторизованного пользователя")
    suspend fun getCurrentUser(): ResponseEntity<UserDto> {
        val tokenInfo = SecurityUtils.getCurrentTokenInfo()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val user = userRepository.findById(tokenInfo.userId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(user.toUserDto().also { log.info("User entity: $it") })
    }

    private suspend fun TokenInfo.toAuthResponse(): AuthResponse {
        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("User not found for token")
        return AuthResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = ChronoUnit.SECONDS.between(Instant.now(), expiresAt),
            user = user.toUserDto()
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(AuthController::class.java)
    }
}

@Schema(description = "Запрос на аутентификацию")
data class AuthRequest(
    @field:Email(message = "Некорректный формат email")
    @field:NotBlank(message = "Email обязателен")
    @field:JsonProperty("email")
    @field:Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @field:Size(min = 8, max = 128, message = "Пароль должен содержать от 8 до 128 символов")
    @field:NotBlank(message = "Пароль обязателен")
    @field:JsonProperty("password")
    @field:Schema(description = "Пароль", example = "Password123!")
    val password: String
)

@Schema(description = "Запрос на регистрацию")
data class RegisterRequest(
    @field:Email(message = "Некорректный формат email")
    @field:NotBlank(message = "Email обязателен")
    @field:Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @field:Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
    @field:NotBlank(message = "Имя обязательно")
    @field:Schema(description = "Имя пользователя", example = "Иван Иванов")
    val name: String,

    @field:ValidPassword
    @field:NotBlank(message = "Пароль обязателен")
    @field:Schema(
        description = "Пароль (минимум 8 символов, заглавная буква, строчная буква, цифра, спецсимвол)",
        example = "Password123!"
    )
    val password: String
)

@Schema(description = "Ответ при успешной аутентификации")
data class AuthResponse(
    @field:Schema(description = "Opaque токен доступа")
    val accessToken: String,

    @field:Schema(description = "Тип токена", example = "Bearer")
    val tokenType: String = "Bearer",

    @field:Schema(description = "Время жизни токена в секундах", example = "86400")
    val expiresIn: Long,

    @field:Schema(description = "Данные пользователя")
    val user: UserDto
)
