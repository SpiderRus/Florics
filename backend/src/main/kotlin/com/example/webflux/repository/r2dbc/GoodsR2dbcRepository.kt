package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.GoodsEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GoodsR2dbcRepository : CoroutineCrudRepository<GoodsEntity, String> {

    @Query("SELECT * FROM goods WHERE deleted_at IS NULL ORDER BY created_at DESC")
    fun findAllActive(): Flow<GoodsEntity>

    @Query("SELECT * FROM goods WHERE id = :id AND deleted_at IS NULL")
    suspend fun findByIdActive(id: String): GoodsEntity?

    @Query("SELECT * FROM goods WHERE category_id = :categoryId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findByCategoryId(categoryId: String): Flow<GoodsEntity>

    @Query("UPDATE goods SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)

    @Query("SELECT * FROM goods WHERE category_id = ANY(:categoryIds) AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findAllByCategoryIdInActive(categoryIds: Array<String>): Flow<GoodsEntity>

    fun findAllByCategoryIdInActive(categoryIds: Collection<String>): Flow<GoodsEntity> =
        findAllByCategoryIdInActive(categoryIds.toTypedArray())
}
