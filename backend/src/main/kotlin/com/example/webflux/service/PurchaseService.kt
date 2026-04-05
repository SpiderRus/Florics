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
    suspend fun recordPurchase(userId: String, goodsId: String, price: BigDecimal): Purchase {
        val purchase = Purchase(
            id = null, // БД сгенерирует ID автоматически
            userId = userId,
            goodsId = goodsId,
            price = price,
            purchaseDate = OffsetDateTime.now()
        )
        return purchaseRepository.save(purchase)
    }

    fun getUserPurchases(userId: String): Flow<Purchase> = purchaseRepository.findByUserId(userId)

    suspend fun hasPurchased(userId: String, goodsId: String): Boolean {
        return purchaseRepository.hasPurchased(userId, goodsId)
    }
}
