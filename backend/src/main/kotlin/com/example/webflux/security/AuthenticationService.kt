package com.example.webflux.security

interface AuthenticationService {
    suspend fun authenticate(email: String, password: String): TokenInfo?
    suspend fun register(email: String, name: String, password: String): TokenInfo
    suspend fun validateToken(token: String): TokenInfo?
    suspend fun revokeToken(token: String): Boolean
}
