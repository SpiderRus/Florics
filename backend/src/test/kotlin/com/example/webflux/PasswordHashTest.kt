package com.example.webflux

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordHashTest {

    @Test
    fun generatePasswordHash() {
        val encoder = BCryptPasswordEncoder()
        val password = "password123"
        val hash = encoder.encode(password)
        println("Password: $password")
        println("BCrypt hash: $hash")
        println("Verification: ${encoder.matches(password, hash)}")
    }
}
