package com.example.webflux.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "Hello", description = "API приветствий с корутинами")
class HelloController {

    @GetMapping("/hello")
    @Operation(summary = "Получить приветствие", description = "Возвращает простое приветственное сообщение с использованием suspend функции")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Успешная операция", content = [Content(schema = Schema(implementation = String::class))])
    ])
    suspend fun hello(): String {
        delay(100) // имитация асинхронной операции
        return "Hello from WebFlux with Coroutines!"
    }

    @GetMapping("/hello/{name}")
    @Operation(summary = "Получить персональное приветствие", description = "Возвращает персонализированное приветствие для указанного имени")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Успешная операция")
    ])
    suspend fun helloName(
        @Parameter(description = "Имя для приветствия", required = true, example = "World")
        @PathVariable name: String
    ): Map<String, String> {
        delay(50)
        return mapOf("message" to "Hello, $name!")
    }

    @GetMapping("/stream")
    @Operation(summary = "Стрим чисел", description = "Потоковая передача чисел от 1 до 10 с задержкой 1 секунда через Flow")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Успешный стрим", content = [Content(schema = Schema(implementation = Int::class))])
    ])
    fun stream(): Flow<Int> = flow {
        for (i in 1..10) {
            delay(1000)
            emit(i)
        }
    }
}
