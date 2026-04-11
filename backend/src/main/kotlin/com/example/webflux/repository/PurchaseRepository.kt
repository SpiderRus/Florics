package com.example.webflux.repository

import com.example.webflux.domain.model.Purchase
import com.example.webflux.entity.PurchaseEntity
import com.example.webflux.mapper.PurchaseMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseR2dbcRepository : CoroutineCrudRepository<PurchaseEntity, String> {

    @Query("SELECT * FROM purchases WHERE user_id = :userId AND deleted_at IS NULL ORDER BY purchase_date DESC")
    fun findByUserId(userId: String): Flow<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE goods_id = :goodsId AND deleted_at IS NULL ORDER BY purchase_date DESC")
    fun findByGoodsId(goodsId: String): Flow<PurchaseEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM purchases WHERE user_id = :userId AND goods_id = :goodsId AND deleted_at IS NULL)")
    suspend fun existsByUserIdAndGoodsId(userId: String, goodsId: String): Boolean

    @Query("UPDATE purchases SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}


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
