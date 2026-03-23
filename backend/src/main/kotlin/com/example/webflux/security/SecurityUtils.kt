package com.example.webflux.security

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication

object SecurityUtils {

    suspend fun getCurrentTokenInfo(): TokenInfo? {
        return ReactiveSecurityContextHolder.getContext()
            .mapNotNull { context ->
                when (val authentication = context.authentication) {
                    // Новый подход с OAuth2 Resource Server
                    is BearerTokenAuthentication -> {
                        val principal = authentication.tokenAttributes
                        TokenInfo(
                            token = authentication.token.tokenValue,
                            userId = principal["sub"]?.toString()?.toLongOrNull() ?: 0L,
                            email = principal["email"]?.toString() ?: "",
                            roles = authentication.authorities.mapNotNull { it.authority?.removePrefix("ROLE_") }.toSet(),
                            createdAt = java.time.Instant.now(), // Не сохраняем в attributes, используем текущее время
                            expiresAt = java.time.Instant.now().plusSeconds(86400) // 24 часа
                        )
                    }
                    else -> null
                }
            }
            .awaitSingleOrNull()
    }

    suspend fun getCurrentUserId(): Long? {
        return getCurrentTokenInfo()?.userId
    }

    suspend fun hasRole(role: String): Boolean {
        return getCurrentTokenInfo()?.roles?.contains(role) ?: false
    }

    suspend fun requireRole(role: String) {
        if (!hasRole(role))
            throw org.springframework.security.access.AccessDeniedException("User does not have required role: $role")
    }
}
