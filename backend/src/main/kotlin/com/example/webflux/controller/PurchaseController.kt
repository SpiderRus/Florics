package com.example.webflux.controller

import com.example.webflux.controller.model.PurchaseDto
import com.example.webflux.controller.model.toPurchaseDto
import com.example.webflux.security.SecurityUtils
import com.example.webflux.service.PurchaseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/purchases")
@Tag(name = "Покупки", description = "API истории покупок товаров")
class PurchaseController(
    private val purchaseService: PurchaseService
) {
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "История покупок", description = "Возвращает список купленных товаров")
    suspend fun getUserPurchases(): Flow<PurchaseDto> =
        purchaseService.getUserPurchases(SecurityUtils.requireCurrentUserId()).map { it.toPurchaseDto() }

    @GetMapping("/has-purchased/{goodsId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Проверка покупки", description = "Проверяет, купил ли пользователь товар")
    suspend fun hasPurchased(@PathVariable goodsId: String): HasPurchasedResponse =
        HasPurchasedResponse(purchaseService.hasPurchased(SecurityUtils.requireCurrentUserId(), goodsId))
}

data class HasPurchasedResponse(val purchased: Boolean)
