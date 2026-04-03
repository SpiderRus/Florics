package com.example.webflux.security

import kotlinx.coroutines.delay
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Repository
class TokenStorage {
    private val tokens = ConcurrentHashMap<String, TokenInfo>()

    suspend fun save(tokenInfo: TokenInfo): TokenInfo {
        tokens[tokenInfo.token] = tokenInfo
        return tokenInfo
    }

    suspend fun findByToken(token: String): TokenInfo? {
        val tokenInfo = tokens[token]
        // Проверка на expiration
        return if (tokenInfo != null && tokenInfo.expiresAt.isAfter(Instant.now()))
            tokenInfo
        else {
            tokens.remove(token)
            null
        }
    }

    suspend fun remove(token: String): Boolean {
        return tokens.remove(token) != null
    }

    suspend fun removeByUserId(userId: String) {
        tokens.entries.removeIf { it.value.userId == userId }
    }
}
