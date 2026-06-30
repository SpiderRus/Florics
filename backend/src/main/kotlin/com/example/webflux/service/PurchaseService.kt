package com.example.webflux.service

import com.example.webflux.domain.model.Purchase
import com.example.webflux.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@Service
class PurchaseService(
    private val purchaseRepository: PurchaseRepository
) {
    suspend fun recordPurchase(
        userId: String,
        goodsId: String,
        price: BigDecimal,
        quantity: Int = 1
    ): Purchase {
        require(quantity > 0) { "Quantity must be positive" }

        val purchase = Purchase(
            id = null, // БД сгенерирует ID автоматически
            userId = userId,
            goodsId = goodsId,
            price = price,
            purchaseDate = OffsetDateTime.now(),
            quantity = quantity
        )
        return purchaseRepository.save(purchase)
    }

    /**
     * Записать кастомный заказ флорариума (нет товара каталога; цену проставит админ).
     * Создаётся со статусом NEW.
     */
    suspend fun recordCustomOrder(
        userId: String,
        conversationId: String,
        imageUrl: String,
        comment: String?,
        contact: String?
    ): Purchase {
        val order = Purchase(
            id = null,
            userId = userId,
            goodsId = null,
            price = null,
            purchaseDate = OffsetDateTime.now(),
            quantity = 1,
            conversationId = conversationId,
            imageUrl = imageUrl,
            customerComment = comment,
            contact = contact,
            status = "NEW"
        )
        return purchaseRepository.save(order)
    }

    fun getUserPurchases(userId: String): Flow<Purchase> = purchaseRepository.findByUserId(userId)

    suspend fun hasPurchased(userId: String, goodsId: String): Boolean {
        return purchaseRepository.hasPurchased(userId, goodsId)
    }
}
