package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект документа Telegram
 * https://core.telegram.org/bots/api#document
 */
@Schema(description = "Документ Telegram")
data class DocumentDto(
    @Schema(description = "Идентификатор файла, который можно использовать для скачивания или повторного использования файла")
    @JsonProperty("file_id")
    val fileId: String,

    @Schema(description = "Уникальный идентификатор файла")
    @JsonProperty("file_unique_id")
    val fileUniqueId: String,

    @Schema(description = "Миниатюра документа, определенная отправителем")
    @JsonProperty("thumbnail")
    val thumbnail: PhotoSizeDto? = null,

    @Schema(description = "Оригинальное имя файла, определенное отправителем")
    @JsonProperty("file_name")
    val fileName: String? = null,

    @Schema(description = "MIME-тип файла, определенный отправителем")
    @JsonProperty("mime_type")
    val mimeType: String? = null,

    @Schema(description = "Размер файла в байтах")
    @JsonProperty("file_size")
    val fileSize: Long? = null
)
