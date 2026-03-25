package com.example.webflux.controller

import com.example.webflux.controller.model.User
import com.example.webflux.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
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
        ApiResponse(responseCode = "200", description = "Успешная операция", content = [Content(schema = Schema(implementation = User::class))])
    ])
    fun getAllUsers(): Flow<User> {
        return userService.getAllUsers()
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает пользователя по его идентификатору")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Пользователь найден", content = [Content(schema = Schema(implementation = User::class))]),
        ApiResponse(responseCode = "404", description = "Пользователь не найден", content = [Content()])
    ])
    suspend fun getUserById(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<User> {
        return userService.getUserById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать пользователя", description = "Создает нового пользователя и возвращает его")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Пользователь создан", content = [Content(schema = Schema(implementation = User::class))]),
        ApiResponse(responseCode = "400", description = "Некорректные данные", content = [Content()])
    ])
    suspend fun createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Данные нового пользователя", required = true)
        @RequestBody user: User
    ): ResponseEntity<User> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить пользователя", description = "Обновляет существующего пользователя по ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Пользователь обновлен", content = [Content(schema = Schema(implementation = User::class))]),
        ApiResponse(responseCode = "404", description = "Пользователь не найден", content = [Content()])
    ])
    suspend fun updateUser(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable id: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Обновленные данные пользователя", required = true)
        @RequestBody user: User
    ): ResponseEntity<User> {
        return userService.updateUser(id, user)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Пользователь удален", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Пользователь не найден", content = [Content()])
    ])
    suspend fun deleteUser(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        return if (userService.deleteUser(id))
            ResponseEntity.noContent().build()
        else
            ResponseEntity.notFound().build()
    }
}
