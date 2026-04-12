package com.example.webflux.security

import com.example.webflux.domain.model.UserRole
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication

object SecurityUtils {
    suspend fun getCurrentTokenInfo(): TokenInfo? {
        return ReactiveSecurityContextHolder.getContext()
            .mapNotNull { context ->
                when (val authentication = context.authentication) {
                    is BearerTokenAuthentication ->
                        // Извлекаем закэшированный TokenInfo из attributes
                        authentication.tokenAttributes["token_info"] as? TokenInfo
                    else -> null
                }
            }
            .awaitSingleOrNull()
    }

    suspend fun getCurrentUserId(): String? = getCurrentTokenInfo()?.userId

    suspend fun requireCurrentUserId(): String = getCurrentUserId() ?: throw IllegalStateException("User not authenticated")

    suspend fun hasRole(role: UserRole): Boolean = getCurrentTokenInfo()?.roles?.contains(role) ?: false
}
