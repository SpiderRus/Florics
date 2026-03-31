package com.example.webflux.repository

import com.example.webflux.domain.model.Purchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
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

    fun findByUserId(userId: Long): Flow<Purchase> = storage[userId]?.asFlow() ?: emptyFlow()

    suspend fun hasPurchased(userId: Long, goodsId: String): Boolean =
        storage[userId]?.any { it.goodsId == goodsId } ?: false
}
