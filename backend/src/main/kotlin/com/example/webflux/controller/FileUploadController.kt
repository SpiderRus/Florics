package com.example.webflux.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("/api/admin/files")
@Tag(name = "Загрузка файлов", description = "API для загрузки изображений и видео (только для администраторов)")
class FileUploadController {

    private val uploadDir = Paths.get("backend/src/main/resources/static/uploads")

    init {
        // Создаем директорию для загрузок если её нет
        Files.createDirectories(uploadDir)
    }

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Загрузить файл",
        description = "Загрузка изображения или видео. Файл сохраняется в директории static/uploads и возвращается URL."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Файл успешно загружен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FileUploadResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Доступ запрещен (требуется роль ADMIN)"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Ошибка загрузки файла"
            )
        ]
    )
    suspend fun uploadFile(
        @RequestPart("file") filePart: FilePart
    ): ResponseEntity<Map<String, String>> {
        return try {
            // Генерируем уникальное имя файла
            val timestamp = System.currentTimeMillis()
            val originalFilename = filePart.filename()
            val filename = "${timestamp}_$originalFilename"
            val filepath = uploadDir.resolve(filename)

            // Сохраняем файл
            filePart.transferTo(filepath).awaitSingleOrNull()

            // Возвращаем URL относительно корня приложения
            val url = "/uploads/$filename"
            ResponseEntity.ok(mapOf("url" to url))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Ошибка загрузки файла: ${e.message}"))
        }
    }
}

@Schema(description = "Ответ при успешной загрузке файла")
data class FileUploadResponse(
    @Schema(description = "URL загруженного файла", example = "/uploads/1234567890_image.jpg")
    val url: String
)
