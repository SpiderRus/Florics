package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.CategoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryR2dbcRepository : CoroutineCrudRepository<CategoryEntity, String> {
    @Query("SELECT * FROM categories WHERE id = :id AND deleted_at IS NULL")
    suspend fun findByIdActive(id: String): CategoryEntity?

    @Query("UPDATE categories SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}
