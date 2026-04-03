package com.example.webflux.security

import com.example.webflux.domain.model.User
import com.example.webflux.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class LocalAuthenticationService(
    private val userRepository: UserRepository,
    private val tokenStorage: TokenStorage,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationService {

    companion object {
        private const val TOKEN_EXPIRATION_HOURS = 8760L
    }

    override suspend fun authenticate(email: String, password: String): TokenInfo? {
        val user = userRepository.findByEmail(email) ?: return null

        if (!passwordEncoder.matches(password, user.password))
            return null

        return generateTokenInfo(user)
    }

    override suspend fun register(email: String, name: String, password: String): TokenInfo {
        // Проверка на существующий email
        if (userRepository.existsByEmail(email))
            throw IllegalArgumentException("Email уже зарегистрирован")

        val hashedPassword = passwordEncoder.encode(password) ?: throw IllegalStateException("Failed to hash password")
        val newUser = User(
            id = generateUserId(),
            name = name,
            email = email,
            password = hashedPassword,
            roles = setOf("USER", "BUYER")
        )

        val savedUser = userRepository.save(newUser)
        return generateTokenInfo(savedUser)
    }

    override suspend fun validateToken(token: String): TokenInfo? {
        return tokenStorage.findByToken(token)
    }

    override suspend fun revokeToken(token: String): Boolean {
        return tokenStorage.remove(token)
    }

    private suspend fun generateTokenInfo(user: User): TokenInfo {
        val token = UUID.randomUUID().toString()
        val now = Instant.now()
        val expiresAt = now.plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS)

        val tokenInfo = TokenInfo(
            token = token,
            userId = user.id,
            email = user.email,
            roles = user.roles,
            createdAt = now,
            expiresAt = expiresAt
        )

        return tokenStorage.save(tokenInfo)
    }

    private fun generateUserId(): String = UUID.randomUUID().toString()
}
