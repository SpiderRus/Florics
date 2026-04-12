package com.example.webflux.security

import com.example.webflux.domain.model.Token
import com.example.webflux.domain.model.User
import com.example.webflux.repository.TokenRepository
import com.example.webflux.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class LocalAuthenticationService(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationService {

    companion object {
        private val log = LoggerFactory.getLogger(LocalAuthenticationService::class.java)
        private const val TOKEN_EXPIRATION_HOURS = 8760L
    }

    @Transactional
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

        val hashedPassword = passwordEncoder.encode(password) ?: throw IllegalStateException("Failed to hash password")

        val newUser = User(
            id = null,
            name = name,
            email = email,
            password = hashedPassword
        )

        val savedUser = userRepository.save(newUser)
        log.info("User saved successfully: id=${savedUser.id}, email=${savedUser.email}")

        val tokenInfo = generateTokenInfo(savedUser)
        log.info("Token generated for user: ${savedUser.email}, token=${tokenInfo.token}")

        return tokenInfo
    }

    override suspend fun validateToken(token: String): TokenInfo? {
        val tokenModel = tokenRepository.findValidToken(token) ?: return null
        val user = userRepository.findById(tokenModel.userId) ?: return null

        return TokenInfo(
            token = tokenModel.token,
            userId = tokenModel.userId,
            email = user.email,
            roles = user.roles,
            createdAt = tokenModel.createdAt,
            expiresAt = tokenModel.expiresAt
        )
    }

    override suspend fun revokeToken(token: String): Boolean {
        return tokenRepository.deleteByToken(token)
    }

    private suspend fun generateTokenInfo(user: User): TokenInfo {
        val now = OffsetDateTime.now()
        val expiresAt = now.plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS)

        // БД сама сгенерирует UUID для токена через DEFAULT gen_random_uuid()
        val token = Token(
            token = "", // Заглушка, БД заменит на сгенерированный UUID
            userId = user.id!!,
            createdAt = now,
            expiresAt = expiresAt
        )

        val savedToken = tokenRepository.save(token)

        return TokenInfo(
            token = savedToken.token,
            userId = user.id!!,
            email = user.email,
            roles = user.roles,
            createdAt = savedToken.createdAt,
            expiresAt = savedToken.expiresAt
        )
    }
}
