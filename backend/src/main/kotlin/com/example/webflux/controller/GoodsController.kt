package com.example.webflux.controller

import com.example.webflux.controller.model.*
import com.example.webflux.domain.model.*
import com.example.webflux.service.GoodsService
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
@RequestMapping("/api/goods")
@Tag(name = "Товары", description = "API управления каталогом товаров")
class GoodsController(
    private val goodsService: GoodsService
) {

    @GetMapping
    @Operation(
        summary = "Получить список всех товаров",
        description = "Возвращает полный каталог товаров (растения, флорариумы, мастер-классы)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров успешно получен",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GoodsDto::class))]
            )
        ]
    )
    fun getAllGoods(): Flow<GoodsDto> = goodsService.getAllGoods().map {
            it.toGoodsDto(goodsService.getCategoryForGoods(it))
        }


    @GetMapping("/search")
    @Operation(
        summary = "Поиск товаров с пагинацией",
        description = "Серверные поиск (по названию/описанию), сортировка и пагинация по типу товара для каталога."
    )
    suspend fun searchGoods(
        @Parameter(description = "Тип товара (PLANT, TERRARIUM, COURSE)", example = "PLANT")
        @RequestParam type: GoodsType,
        @Parameter(description = "Текст поиска по названию/описанию")
        @RequestParam(required = false) query: String?,
        @Parameter(description = "Поле сортировки (name, price, created_at)", example = "name")
        @RequestParam(defaultValue = "created_at") sortBy: String,
        @Parameter(description = "Направление (asc, desc)", example = "asc")
        @RequestParam(defaultValue = "desc") sortOrder: String,
        @Parameter(description = "Номер страницы (с 0)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Размер страницы (1-50)", example = "12")
        @RequestParam(defaultValue = "12") size: Int
    ): ResponseEntity<PagedGoodsResponse> {
        require(page >= 0) { "Page must be >= 0" }
        require(size in 1..50) { "Size must be between 1 and 50" }
        return ResponseEntity.ok(
            goodsService.searchGoodsByType(type, query, sortBy, sortOrder.lowercase(), page, size)
        )
    }


    @GetMapping("/{id}")
    @Operation(
        summary = "Получить товар по ID",
        description = "Возвращает подробную информацию о конкретном товаре"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар найден",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GoodsDto::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Товар не найден"
            )
        ]
    )
    suspend fun getGoodsById(
        @Parameter(description = "ID товара", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: String
    ): ResponseEntity<GoodsDto> =
        goodsService.getGoodsById(id)?.let {
            ResponseEntity.ok(it.toGoodsDto(goodsService.getCategoryForGoods(it)))
        } ?: ResponseEntity.notFound().build()


    @GetMapping("/type/{type}")
    @Operation(
        summary = "Получить товары по типу",
        description = "Возвращает список товаров указанного типа (PLANT, TERRARIUM, COURSE)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров успешно получен",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GoodsDto::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Некорректный тип товара"
            )
        ]
    )
    fun getGoodsByType(
        @Parameter(description = "Тип товара (PLANT, TERRARIUM, COURSE)", example = "PLANT")
        @PathVariable type: GoodsType): Flow<GoodsDto> =
            goodsService.getGoodsByType(type).map {
                it.toGoodsDto(goodsService.getCategoryForGoods(it))
            }
}
