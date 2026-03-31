package com.example.webflux.service

import com.example.webflux.domain.model.Purchase
import com.example.webflux.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class PurchaseService(
    private val purchaseRepository: PurchaseRepository
) {
    suspend fun recordPurchase(userId: Long, goodsId: String, price: Double): Purchase {
        val purchase = Purchase(
            id = UUID.randomUUID().toString(),
            userId = userId,
            goodsId = goodsId,
            price = price,
            purchaseDate = Instant.now()
        )
        return purchaseRepository.save(purchase)
    }

    fun getUserPurchases(userId: Long): Flow<Purchase> = purchaseRepository.findByUserId(userId)

    suspend fun hasPurchased(userId: Long, goodsId: String): Boolean {
        return purchaseRepository.hasPurchased(userId, goodsId)
    }
}
