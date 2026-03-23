package com.example.webflux.controller

import com.example.webflux.controller.model.Plant
import com.example.webflux.service.PlantService
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
@RequestMapping("/api/plants")
@Tag(name = "Растения", description = "API управления каталогом растений")
class PlantController(
    private val plantService: PlantService
) {

    @GetMapping
    @Operation(
        summary = "Получить список всех растений",
        description = "Возвращает полный каталог комнатных растений с фотографиями, описаниями и ценами"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список растений успешно получен",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Plant::class))]
            )
        ]
    )
    suspend fun getAllPlants(): ResponseEntity<List<Plant>> {
        val plants = plantService.getAllPlants()
        return ResponseEntity.ok(plants)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить растение по ID",
        description = "Возвращает подробную информацию о конкретном растении"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Растение найдено",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Plant::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Растение не найдено"
            )
        ]
    )
    suspend fun getPlantById(
        @Parameter(description = "ID растения", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Plant> {
        val plant = plantService.getPlantById(id)
        return if (plant != null)
            ResponseEntity.ok(plant)
        else
            ResponseEntity.notFound().build()
    }
}
