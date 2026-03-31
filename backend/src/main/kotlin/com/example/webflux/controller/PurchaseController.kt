package com.example.webflux.controller

import com.example.webflux.domain.model.Purchase
import com.example.webflux.controller.model.PurchaseDto
import com.example.webflux.controller.model.PurchasesResponseDto
import com.example.webflux.security.SecurityUtils
import com.example.webflux.service.PurchaseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/purchases")
@Tag(name = "Покупки", description = "API истории покупок курсов")
class PurchaseController(
    private val purchaseService: PurchaseService
) {
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "История покупок", description = "Возвращает список купленных курсов")
    suspend fun getUserPurchases(): ResponseEntity<PurchasesResponseDto> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("User not authenticated")
        val purchases = purchaseService.getUserPurchases(userId).map { it.toDto() }
        return ResponseEntity.ok(PurchasesResponseDto(purchases))
    }

    @GetMapping("/has-purchased/{goodsId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Проверка покупки", description = "Проверяет, купил ли пользователь курс")
    suspend fun hasPurchased(@PathVariable goodsId: String): ResponseEntity<HasPurchasedResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("User not authenticated")
        val purchased = purchaseService.hasPurchased(userId, goodsId)
        return ResponseEntity.ok(HasPurchasedResponse(purchased))
    }

    // Extension функция для преобразования domain entity в DTO
    private fun Purchase.toDto() = PurchaseDto(
        id = id,
        goodsId = goodsId,
        price = price,
        purchaseDate = purchaseDate,
        quantity = quantity
    )
}

data class HasPurchasedResponse(val purchased: Boolean)
