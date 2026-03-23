package com.example.webflux.security

interface AuthorizationService {
    suspend fun hasRole(tokenInfo: TokenInfo, role: String): Boolean
    suspend fun hasAnyRole(tokenInfo: TokenInfo, roles: Set<String>): Boolean
}
