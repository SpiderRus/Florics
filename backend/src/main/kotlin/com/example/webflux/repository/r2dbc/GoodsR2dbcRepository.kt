package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.GoodsEntity
import com.example.webflux.entity.MediaEntity
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

    @Query("""
        SELECT * FROM goods
        WHERE deleted_at IS NULL
        ORDER BY
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN name END DESC,
            CASE WHEN :sortBy = 'price' AND :sortOrder = 'ASC' THEN price END ASC,
            CASE WHEN :sortBy = 'price' AND :sortOrder = 'DESC' THEN price END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN created_at END DESC
        LIMIT :limit OFFSET :offset
    """)
    fun findAllActivePaged(
        sortBy: String,
        sortOrder: String,
        limit: Int,
        offset: Int
    ): Flow<GoodsEntity>

    @Query("SELECT COUNT(*) FROM goods WHERE deleted_at IS NULL")
    suspend fun countActive(): Long

    @Query("SELECT * FROM goods WHERE id = ANY(:ids) AND deleted_at IS NULL")
    fun findByIdsIn(ids: Array<String>): Flow<GoodsEntity>
}
