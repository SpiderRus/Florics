package com.example.webflux.domain.model

data class Goods(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val images: List<String>,
    val category: String,
    val difficulty: String,
    val type: String? = null,
    val duration: Int? = null,
    val videoUrl: String? = null,
    val videoGalleryUrls: List<String>? = null,
    val previewUrl: String? = null,
    val detailedDescription: String? = null,
    val careInstructions: String? = null
)
