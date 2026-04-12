package com.example.webflux.security

import com.example.webflux.domain.model.UserRole

interface AuthorizationService {
    suspend fun hasRole(tokenInfo: TokenInfo, role: UserRole): Boolean
    suspend fun hasAnyRole(tokenInfo: TokenInfo, roles: Set<UserRole>): Boolean
}
