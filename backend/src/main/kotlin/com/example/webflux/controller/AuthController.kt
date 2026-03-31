package com.example.webflux.controller

import com.example.webflux.domain.model.User
import com.example.webflux.controller.model.*
import com.example.webflux.repository.UserRepository
import com.example.webflux.security.AuthenticationService
import com.example.webflux.security.SecurityUtils
import com.example.webflux.security.TokenInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
    suspend fun login(@RequestBody request: AuthRequest): ResponseEntity<AuthResponse> {
        val tokenInfo = authenticationService.authenticate(request.email, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(tokenInfo.toAuthResponse())
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Создание нового пользователя")
    suspend fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
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

        return ResponseEntity.ok(user.toDto())
    }

    private suspend fun TokenInfo.toAuthResponse(): AuthResponse {
        val user = userRepository.findById(userId)
        return AuthResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = ChronoUnit.SECONDS.between(Instant.now(), expiresAt),
            user = UserDto(userId, user?.name ?: "", email, roles)
        )
    }

    private fun User.toDto() = UserDto(id, name, email, roles)
}
