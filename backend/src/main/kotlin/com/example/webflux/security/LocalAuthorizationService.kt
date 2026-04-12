package com.example.webflux.security

import com.example.webflux.domain.model.UserRole
import org.springframework.stereotype.Service

@Service
class LocalAuthorizationService : AuthorizationService {

    override suspend fun hasRole(tokenInfo: TokenInfo, role: UserRole): Boolean {
        return tokenInfo.roles.contains(role)
    }

    override suspend fun hasAnyRole(tokenInfo: TokenInfo, roles: Set<UserRole>): Boolean {
        return tokenInfo.roles.any { it in roles }
    }
}
