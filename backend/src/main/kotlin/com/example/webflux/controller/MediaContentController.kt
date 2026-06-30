package com.example.webflux.controller

import com.example.webflux.service.MediaService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class MediaContentController(
    private val mediaService: MediaService
) {
    /** Публичная отдача бинарного содержимого фото из БД. Байты по id неизменны → immutable-кэш. */
    @GetMapping("/api/media/{id}")
    suspend fun get(@PathVariable id: String): ResponseEntity<ByteArray> {
        val c = mediaService.loadContent(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, c.contentType)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
            .body(c.content)
    }
}
