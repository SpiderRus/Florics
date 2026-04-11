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

    suspend fun save(purchase: Purchase): Purchase =
        PurchaseMapper.toModel(purchaseR2dbcRepository.save(PurchaseMapper.toEntity(purchase)))

    fun findByUserId(userId: String): Flow<Purchase> =
        purchaseR2dbcRepository.findByUserId(userId).map { PurchaseMapper.toModel(it) }

    suspend fun hasPurchased(userId: String, goodsId: String): Boolean =
        purchaseR2dbcRepository.existsByUserIdAndGoodsId(userId, goodsId)
}
