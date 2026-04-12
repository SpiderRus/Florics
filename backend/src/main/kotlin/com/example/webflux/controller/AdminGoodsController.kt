package com.example.webflux.controller

import com.example.webflux.controller.model.*
import com.example.webflux.domain.model.UserRole
import com.example.webflux.service.GoodsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/goods")
@Tag(name = "Админ - Товары", description = "API управления товарами для администраторов")
class AdminGoodsController(
    private val goodsService: GoodsService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить список товаров с пагинацией (админ)",
        description = "Возвращает товары с поддержкой сортировки и пагинации. Доступно только администраторам."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров успешно получен",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PagedGoodsResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Некорректные параметры запроса"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Доступ запрещен (требуется роль ADMIN)"
            )
        ]
    )
    suspend fun getAllGoods(
        @Parameter(description = "Номер страницы (с 0)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Размер страницы (5-50)", example = "10")
        @RequestParam(defaultValue = "10") size: Int,

        @Parameter(description = "Поле сортировки", example = "name")
        @RequestParam(defaultValue = "created_at") sortBy: String,

        @Parameter(description = "Направление сортировки", example = "desc")
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<PagedGoodsResponse> {
        // Валидация параметров
        require(page >= 0) { "Page must be >= 0" }
        require(size in 5..50) { "Size must be between 5 and 50" }
        require(sortBy in listOf("name", "category", "price", "created_at")) {
            "Invalid sortBy field: $sortBy"
        }
        require(sortOrder.lowercase() in listOf("asc", "desc")) {
            "Invalid sortOrder: $sortOrder"
        }

        val pagedGoods = goodsService.getGoodsPaged(page, size, sortBy, sortOrder.lowercase())

        return ResponseEntity.ok(pagedGoods)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Создать товар",
        description = "Создание нового товара. Доступно только администраторам."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Товар успешно создан",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GoodsDto::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Некорректные данные"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Доступ запрещен (требуется роль ADMIN)"
            )
        ]
    )
    suspend fun createGoods(
        @Valid @RequestBody request: CreateGoodsRequest
    ): ResponseEntity<GoodsDto> {
        val created = goodsService.createGoods(request)
        val category = goodsService.getCategoryForGoods(created)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(created.toGoodsDto(category))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Обновить товар",
        description = "Обновление существующего товара. Доступно только администраторам."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно обновлен",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GoodsDto::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Товар не найден"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Доступ запрещен (требуется роль ADMIN)"
            )
        ]
    )
    suspend fun updateGoods(
        @Parameter(description = "ID товара", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateGoodsRequest
    ): ResponseEntity<GoodsDto> {
        val updated = goodsService.updateGoods(id, request)
            ?: return ResponseEntity.notFound().build()

        val category = goodsService.getCategoryForGoods(updated)
        return ResponseEntity.ok(updated.toGoodsDto(category))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Удалить товар",
        description = "Soft delete товара. Доступно только администраторам."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Товар успешно удален"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Товар не найден"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Доступ запрещен (требуется роль ADMIN)"
            )
        ]
    )
    suspend fun deleteGoods(
        @Parameter(description = "ID товара", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val deleted = goodsService.deleteGoods(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
