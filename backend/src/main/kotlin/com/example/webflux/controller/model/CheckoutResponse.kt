package com.example.webflux.controller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ при оформлении заказа")
data class CheckoutResponse(
    @Schema(description = "Успешность операции")
    val success: Boolean,

    @Schema(description = "Сообщение о результате")
    val message: String,

    @Schema(description = "Список ID купленных курсов")
    val purchasedCourses: List<String>,

    @Schema(description = "Общая сумма покупки")
    val totalAmount: Double
)
