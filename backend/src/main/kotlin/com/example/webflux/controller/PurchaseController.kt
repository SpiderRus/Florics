package com.example.webflux.controller

import com.example.webflux.model.Purchase
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
    suspend fun getUserPurchases(): ResponseEntity<PurchasesResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("User not authenticated")
        val purchases = purchaseService.getUserPurchases(userId)
        return ResponseEntity.ok(PurchasesResponse(purchases))
    }

    @GetMapping("/has-purchased/{plantId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Проверка покупки", description = "Проверяет, купил ли пользователь курс")
    suspend fun hasPurchased(@PathVariable plantId: String): ResponseEntity<HasPurchasedResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("User not authenticated")
        val purchased = purchaseService.hasPurchased(userId, plantId)
        return ResponseEntity.ok(HasPurchasedResponse(purchased))
    }
}

data class PurchasesResponse(val purchases: List<Purchase>)
data class HasPurchasedResponse(val purchased: Boolean)
