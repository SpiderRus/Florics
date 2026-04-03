package com.example.webflux.repository

import com.example.webflux.domain.model.Purchase
import com.example.webflux.mapper.PurchaseMapper
import com.example.webflux.repository.r2dbc.PurchaseR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Repository

@Repository
class PurchaseRepository(
    private val purchaseR2dbcRepository: PurchaseR2dbcRepository
) {

    suspend fun save(purchase: Purchase): Purchase {
        val entity = PurchaseMapper.toEntity(purchase)
        val saved = purchaseR2dbcRepository.save(entity)
        return PurchaseMapper.toModel(saved)
    }

    fun findByUserId(userId: String): Flow<Purchase> {
        return purchaseR2dbcRepository.findByUserId(userId)
            .map { PurchaseMapper.toModel(it) }
    }

    suspend fun hasPurchased(userId: String, goodsId: String): Boolean =
        purchaseR2dbcRepository.existsByUserIdAndGoodsId(userId, goodsId)
}
