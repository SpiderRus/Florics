package com.example.webflux.security

import com.example.webflux.domain.model.User
import com.example.webflux.repository.UserRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class LocalAuthenticationService(
    private val userRepository: UserRepository,
    private val tokenStorage: TokenStorage,
    private val passwordEncoder: PasswordEncoder,
    private val transactionalOperator: TransactionalOperator
) : AuthenticationService {

    companion object {
        private val log = LoggerFactory.getLogger(LocalAuthenticationService::class.java)
        private const val TOKEN_EXPIRATION_HOURS = 8760L
    }

    override suspend fun authenticate(email: String, password: String): TokenInfo? {
        val user = userRepository.findByEmail(email) ?: return null

        if (!passwordEncoder.matches(password, user.password))
            return null

        return generateTokenInfo(user)
    }

    @Transactional
    override suspend fun register(email: String, name: String, password: String): TokenInfo {
        log.info("Starting registration for email: $email")

        // Проверка на существующий email
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: email already exists - $email")
            throw IllegalArgumentException("Email уже зарегистрирован")
        }

        log.debug("Hashing password for user: $email")
        val hashedPassword = passwordEncoder.encode(password) ?: throw IllegalStateException("Failed to hash password")

        val newUser = User(
            id = null,
            name = name,
            email = email,
            password = hashedPassword,
            roles = setOf("USER", "BUYER")
        )

        val savedUser = userRepository.save(newUser)
        log.info("User saved successfully: id=${savedUser.id}, email=${savedUser.email}")

        val tokenInfo = generateTokenInfo(savedUser)
        log.info("Token generated for user: ${savedUser.email}, token=${tokenInfo.token}")

        return tokenInfo
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
            userId = user.id!!,
            email = user.email,
            roles = user.roles,
            createdAt = now,
            expiresAt = expiresAt
        )

        return tokenStorage.save(tokenInfo)
    }
}
