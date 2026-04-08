package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.TokenEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime

@Repository
interface TokenR2dbcRepository : CoroutineCrudRepository<TokenEntity, String> {

    @Query("SELECT * FROM tokens WHERE token = :token AND expires_at > :now")
    suspend fun findValidToken(token: String, now: OffsetDateTime): TokenEntity?

    @Query("SELECT * FROM tokens WHERE user_id = :userId AND expires_at > :now")
    suspend fun findValidTokensByUserId(userId: String, now: OffsetDateTime): List<TokenEntity>

    @Modifying
    @Query("DELETE FROM tokens WHERE expires_at <= :now")
    suspend fun deleteExpiredTokens(now: OffsetDateTime): Int

    @Modifying
    @Query("DELETE FROM tokens WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String): Int
}
