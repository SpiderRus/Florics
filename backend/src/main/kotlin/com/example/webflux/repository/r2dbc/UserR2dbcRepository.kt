package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserR2dbcRepository : CoroutineCrudRepository<UserEntity, String> {

    @Query("SELECT * FROM users WHERE email = :email AND deleted_at IS NULL")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE deleted_at IS NULL")
    fun findAllActive(): Flow<UserEntity>

    @Query("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}
