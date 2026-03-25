package com.example.webflux.service

import com.example.webflux.model.Purchase
import com.example.webflux.repository.PurchaseRepository
import com.example.webflux.repository.PlantRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class PurchaseService(
    private val purchaseRepository: PurchaseRepository,
    private val plantRepository: PlantRepository
) {
    suspend fun recordPurchase(userId: Long, plantId: String, price: Double): Purchase {
        val purchase = Purchase(
            id = UUID.randomUUID().toString(),
            userId = userId,
            plantId = plantId,
            price = price,
            purchaseDate = Instant.now()
        )
        return purchaseRepository.save(purchase)
    }

    suspend fun getUserPurchases(userId: Long): List<Purchase> {
        return purchaseRepository.findByUserId(userId)
    }

    suspend fun hasPurchased(userId: Long, plantId: String): Boolean {
        return purchaseRepository.hasPurchased(userId, plantId)
    }
}
