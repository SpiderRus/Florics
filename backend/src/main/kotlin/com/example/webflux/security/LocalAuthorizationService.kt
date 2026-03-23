package com.example.webflux.security

import org.springframework.stereotype.Service

@Service
class LocalAuthorizationService : AuthorizationService {

    override suspend fun hasRole(tokenInfo: TokenInfo, role: String): Boolean {
        return tokenInfo.roles.contains(role)
    }

    override suspend fun hasAnyRole(tokenInfo: TokenInfo, roles: Set<String>): Boolean {
        return tokenInfo.roles.any { it in roles }
    }
}
