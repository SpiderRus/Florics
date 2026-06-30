package com.example.webflux.service.aibot.dto

/**
 * Карточка растения от бота photo-analyzer. Отдаётся фронту как есть (camelCase).
 * На успехе name..care заполнены, error=null. На неудаче — content пуст, error содержит причину.
 */
data class PlantCardDto(
    val name: String? = null,
    val shortDescription: String? = null,
    val fullDescription: String? = null,
    val care: String? = null,
    val error: String? = null
)
