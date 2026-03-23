package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект размера фотографии Telegram
 * https://core.telegram.org/bots/api#photosize
 */
@Schema(description = "Размер фотографии Telegram")
data class PhotoSizeDto(
    @Schema(description = "Идентификатор файла, который можно использовать для скачивания или повторного использования файла")
    @JsonProperty("file_id")
    val fileId: String,

    @Schema(description = "Уникальный идентификатор файла")
    @JsonProperty("file_unique_id")
    val fileUniqueId: String,

    @Schema(description = "Ширина фотографии")
    @JsonProperty("width")
    val width: Int,

    @Schema(description = "Высота фотографии")
    @JsonProperty("height")
    val height: Int,

    @Schema(description = "Размер файла в байтах")
    @JsonProperty("file_size")
    val fileSize: Long? = null
)
