package com.example.webflux.controller.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Медиа элемент (изображение или видео)")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ImageDto::class, name = "image"),
    JsonSubTypes.Type(value = VideoDto::class, name = "video")
)
sealed interface MediaDto {
    val type: String
    val url: String
    val order: Int
}

@Schema(description = "Изображение")
data class ImageDto(
    override val type: String = "image",
    @Schema(description = "URL изображения")
    override val url: String,
    @Schema(description = "Порядок отображения")
    override val order: Int = 0
) : MediaDto

@Schema(description = "Видео")
data class VideoDto(
    override val type: String = "video",
    @Schema(description = "URL видео")
    override val url: String,
    @Schema(description = "Порядок отображения")
    override val order: Int = 0
) : MediaDto
