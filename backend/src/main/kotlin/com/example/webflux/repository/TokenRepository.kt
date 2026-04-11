package com.example.webflux.repository

import com.example.webflux.domain.model.Token
import com.example.webflux.entity.TokenEntity
import com.example.webflux.mapper.TokenMapper
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

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
    suspend fun deleteByUserId(userId: String): Int?
}


@Repository
class TokenRepository(
    private val tokenR2dbcRepository: TokenR2dbcRepository
) {
    /**
     * Сохранить токен в БД
     */
    suspend fun save(token: Token): Token {
        log.debug("Saving token: ${token.token} for userId: ${token.userId}")
        return TokenMapper.toModel(tokenR2dbcRepository.save(TokenMapper.toEntity(token)))
    }

    /**
     * Найти токен по значению и проверить его валидность
     * Возвращает null если токен не найден или истек
     */
    suspend fun findValidToken(token: String): Token? {
        val entity = tokenR2dbcRepository.findValidToken(token, OffsetDateTime.now())

        return if (entity == null) {
                log.debug("Token not found or expired: $token")
                null
            } else
                TokenMapper.toModel(entity)
    }

    /**
     * Найти все валидные токены пользователя
     */
    suspend fun findValidTokensByUserId(userId: String): List<Token> =
       tokenR2dbcRepository.findValidTokensByUserId(userId, OffsetDateTime.now())
           .map { TokenMapper.toModel(it) }

    /**
     * Удалить конкретный токен
     */
    suspend fun deleteByToken(token: String): Boolean =
        try {
            tokenR2dbcRepository.deleteById(token).let { true }.also { log.debug("Token deleted: $token") }
        } catch (e: Exception) {
            log.warn("Failed to delete token: $token", e)
            false
        }

    /**
     * Удалить все токены пользователя (при logout из всех устройств)
     */
    suspend fun deleteByUserId(userId: String): Int {
        val count = tokenR2dbcRepository.deleteByUserId(userId) ?: 0
        return count
    }

    /**
     * Очистить все истекшие токены (для периодической очистки)
     */
    suspend fun deleteExpiredTokens(): Int {
        val count = tokenR2dbcRepository.deleteExpiredTokens(OffsetDateTime.now())
        if (count > 0)
            log.info("Deleted $count expired tokens")
        return count
    }

    /**
     * Проверить существование валидного токена
     */
    suspend fun existsValidToken(token: String): Boolean = findValidToken(token) != null

    private companion object {
        private val log = LoggerFactory.getLogger(TokenRepository::class.java)
    }
}
