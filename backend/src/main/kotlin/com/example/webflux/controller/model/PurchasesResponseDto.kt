package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Список покупок пользователя")
data class PurchasesResponseDto(
    @Schema(description = "Список покупок")
    val purchases: List<PurchaseDto>
)
