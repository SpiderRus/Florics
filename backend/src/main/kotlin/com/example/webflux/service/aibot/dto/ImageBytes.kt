package com.example.webflux.service.aibot.dto

/** Резолвленное изображение (байты + mime) для отправки боту. */
data class ImageBytes(
    val bytes: ByteArray,
    val contentType: String
)
