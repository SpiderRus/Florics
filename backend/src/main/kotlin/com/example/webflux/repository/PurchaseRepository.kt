package com.example.webflux.repository

import com.example.webflux.repository.model.Purchase
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class PurchaseRepository {
    // userId -> List<Purchase>
    private val storage = ConcurrentHashMap<Long, MutableList<Purchase>>()

    suspend fun save(purchase: Purchase): Purchase {
        storage.getOrPut(purchase.userId) { mutableListOf() }.add(purchase)
        return purchase
    }

    suspend fun findByUserId(userId: Long): List<Purchase> {
        return storage[userId]?.toList() ?: emptyList()
    }

    suspend fun hasPurchased(userId: Long, plantId: String): Boolean {
        return storage[userId]?.any { it.plantId == plantId } ?: false
    }
}
