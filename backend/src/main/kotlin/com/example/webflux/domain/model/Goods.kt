package com.example.webflux.domain.model

data class Goods(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val media: List<Media>,
    val categoryId: String,
    val difficulty: String,
    val duration: Int? = null,
    val videoUrl: String? = null,
    val previewUrl: String? = null,
    val detailedDescription: String? = null,
    val careInstructions: String? = null
)
