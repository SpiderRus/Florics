package com.example.webflux.domain.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Image::class, name = "image"),
    JsonSubTypes.Type(value = Video::class, name = "video")
)
sealed interface Media {
    val url: String
    val order: Int
}

data class Image(
    override val url: String,
    override val order: Int = 0
) : Media

data class Video(
    override val url: String,
    override val order: Int = 0
) : Media
