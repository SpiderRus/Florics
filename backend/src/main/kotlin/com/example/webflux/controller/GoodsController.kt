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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
            it.toDto(goodsService.getCategoryForGoods(it))
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
        @Parameter(description = "ID товара", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<GoodsDto> =
        goodsService.getGoodsById(id)?.let {
            ResponseEntity.ok(it.toDto(goodsService.getCategoryForGoods(it)))
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
        @PathVariable type: GoodsType
    ): Flow<GoodsDto> = goodsService.getGoodsByType(type).map { it.toDto(goodsService.getCategoryForGoods(it)) }

    // Extension функция для преобразования Media в MediaDto
    private fun Media.toDto(): MediaDto = when (this) {
        is Image -> ImageDto(url = url, order = order)
        is Video -> VideoDto(url = url, order = order)
    }

    // Extension функция для преобразования domain entity в DTO
    private fun Goods.toDto(category: Category?) = GoodsDto(
        id = id,
        name = name,
        description = description,
        price = price,
        media = media.map { it.toDto() }.sortedBy { it.order },
        category = category?.let { CategoryDto(it.id, it.name, it.type) },
        difficulty = difficulty,
        duration = duration,
        videoUrl = videoUrl,
        previewUrl = previewUrl,
        detailedDescription = detailedDescription,
        careInstructions = careInstructions
    )
}
