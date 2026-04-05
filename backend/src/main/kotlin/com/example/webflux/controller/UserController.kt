package com.example.webflux.controller

import com.example.webflux.controller.model.*
import com.example.webflux.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи", description = "API управления пользователями с корутинами и Flow")
class UserController(private val userService: UserService) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получить всех пользователей", description = "Возвращает поток всех пользователей через Flow")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Успешная операция", content = [Content(schema = Schema(implementation = UserResponseDto::class))])
    ])
    fun getAllUsers(): Flow<UserResponseDto> {
        return userService.getAllUsers().map { it.toUserResponseDto() }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает пользователя по его идентификатору")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Пользователь найден", content = [Content(schema = Schema(implementation = UserResponseDto::class))]),
        ApiResponse(responseCode = "404", description = "Пользователь не найден", content = [Content()])
    ])
    suspend fun getUserById(
        @Parameter(description = "ID пользователя", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: String
    ): ResponseEntity<UserResponseDto> {
        return userService.getUserById(id)
            ?.let { ResponseEntity.ok(it.toUserResponseDto()) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать пользователя", description = "Создает нового пользователя и возвращает его")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Пользователь создан", content = [Content(schema = Schema(implementation = UserResponseDto::class))]),
        ApiResponse(responseCode = "400", description = "Некорректные данные", content = [Content()])
    ])
    suspend fun createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Данные нового пользователя", required = true)
        @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponseDto> {
        // БД сгенерирует ID автоматически
        val user = request.toDomain(null)
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user).toUserResponseDto())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить пользователя", description = "Обновляет существующего пользователя по ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Пользователь обновлен", content = [Content(schema = Schema(implementation = UserResponseDto::class))]),
        ApiResponse(responseCode = "404", description = "Пользователь не найден", content = [Content()])
    ])
    suspend fun updateUser(
        @Parameter(description = "ID пользователя", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: String,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Обновленные данные пользователя", required = true)
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponseDto> {
        val existingUser = userService.getUserById(id) ?: return ResponseEntity.notFound().build()
        val updatedUser = request.applyTo(existingUser)
        return userService.updateUser(id, updatedUser)
            ?.let { ResponseEntity.ok(it.toUserResponseDto()) }
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Пользователь удален", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Пользователь не найден", content = [Content()])
    ])
    suspend fun deleteUser(
        @Parameter(description = "ID пользователя", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: String
    ): ResponseEntity<Void> {
        return if (userService.deleteUser(id))
            ResponseEntity.noContent().build()
        else
            ResponseEntity.notFound().build()
    }

}
