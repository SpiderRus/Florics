package com.example.webflux.controller

import com.example.webflux.controller.model.AnalyzeByNameRequest
import com.example.webflux.service.PhotoAnalyzeService
import com.example.webflux.service.aibot.dto.PlantCardDto
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

/**
 * Анализ фотографий товара ботом photo-analyzer (вкладка «Фотографии»).
 * Принимает multipart: file-части `files` (blob-фото) + поля `mediaIds`/`urls` (JSON-массивы
 * ссылок на сохранённые/внешние фото). Возвращает PlantCard для автозаполнения формы.
 */
@RestController
@RequestMapping("/api/admin/goods")
@PreAuthorize("hasRole('ADMIN')")
class AdminPhotoAnalyzeController(
    private val photoAnalyzeService: PhotoAnalyzeService
) {
    @PostMapping("/analyze-photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun analyze(exchange: ServerWebExchange): PlantCardDto {
        val parts = exchange.multipartData.awaitSingle()
        val files = parts["files"]?.filterIsInstance<FilePart>() ?: emptyList()
        val mediaIds = (parts["mediaIds"]?.firstOrNull() as? FormFieldPart)?.value()
        val urls = (parts["urls"]?.firstOrNull() as? FormFieldPart)?.value()
        return photoAnalyzeService.analyze(files, mediaIds, urls)
    }

    /**
     * Заполнить карточку по названию товара (без изображений) — кнопка рядом с полем «Название».
     * Принимает JSON {name}, возвращает PlantCard для автозаполнения формы.
     */
    @PostMapping("/analyze-name", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun analyzeByName(@RequestBody request: AnalyzeByNameRequest): PlantCardDto =
        photoAnalyzeService.analyzeByName(request.name)
}
