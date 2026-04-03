package com.example.webflux.controller

import com.example.webflux.controller.model.CategoryDto
import com.example.webflux.controller.model.toCategoryDto
import com.example.webflux.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Категории", description = "API управления категориями товаров")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    @Operation(
        summary = "Получить список всех категорий",
        description = "Возвращает полный список категорий товаров"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список категорий успешно получен",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = CategoryDto::class))]
            )
        ]
    )
    suspend fun getAllCategories(): List<CategoryDto> = categoryService.getAllCategories().map { it.toCategoryDto() }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить категорию по ID",
        description = "Возвращает информацию о конкретной категории"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Категория найдена",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = CategoryDto::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Категория не найдена"
            )
        ]
    )
    suspend fun getCategoryById(
        @Parameter(description = "ID категории", example = "1")
        @PathVariable id: String
    ): ResponseEntity<CategoryDto> =
        categoryService.getCategoryById(id)?.let { ResponseEntity.ok(it.toCategoryDto()) } ?: ResponseEntity.notFound().build()
}
