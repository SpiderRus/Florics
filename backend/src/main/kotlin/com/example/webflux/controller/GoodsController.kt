package com.example.webflux.controller

import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.Category
import com.example.webflux.controller.model.GoodsDto
import com.example.webflux.controller.model.CategoryDto
import com.example.webflux.service.GoodsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
    suspend fun getAllGoods(): ResponseEntity<List<GoodsDto>> {
        val goods = goodsService.getAllGoods().map {
            val category = goodsService.getCategoryForGoods(it)
            it.toDto(category)
        }
        return ResponseEntity.ok(goods)
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
    ): ResponseEntity<GoodsDto> {
        val goods = goodsService.getGoodsById(id)
        return goods?.let {
            val category = goodsService.getCategoryForGoods(it)
            ResponseEntity.ok(it.toDto(category))
        } ?: ResponseEntity.notFound().build()
    }

    // Extension функция для преобразования domain entity в DTO
    private fun Goods.toDto(category: Category?) = GoodsDto(
        id = id,
        name = name,
        description = description,
        price = price,
        images = images,
        categoryId = categoryId,
        category = category?.let { CategoryDto(it.id, it.name, it.type) },
        difficulty = difficulty,
        duration = duration,
        videoUrl = videoUrl,
        videoGalleryUrls = videoGalleryUrls,
        previewUrl = previewUrl,
        detailedDescription = detailedDescription,
        careInstructions = careInstructions
    )
}
