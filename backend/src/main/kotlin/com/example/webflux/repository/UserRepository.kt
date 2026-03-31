package com.example.webflux.repository

import com.example.webflux.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.stereotype.Repository

@Repository
class UserRepository {

    // BCrypt hash для "password123"
    private val storage = mutableMapOf(
        1L to User(1, "Alice", "alice@example.com", "\$2a\$10\$GLV9oczk1dvKX9vJ2gHlhOn41DInakVQiRSDzJehbIE7Uqr2VLZIy", setOf("USER", "BUYER")),
        2L to User(2, "Bob", "bob@example.com", "\$2a\$10\$GLV9oczk1dvKX9vJ2gHlhOn41DInakVQiRSDzJehbIE7Uqr2VLZIy", setOf("USER", "BUYER")),
        3L to User(3, "Admin", "admin@example.com", "\$2a\$10\$GLV9oczk1dvKX9vJ2gHlhOn41DInakVQiRSDzJehbIE7Uqr2VLZIy", setOf("USER", "ADMIN", "BUYER"))
    )

    suspend fun findById(id: Long): User? {
        return storage[id]
    }

    suspend fun save(user: User): User {
        storage[user.id] = user
        return user
    }

    fun findAll(): Flow<User> {
        return storage.values.asFlow()
    }

    suspend fun deleteById(id: Long): Boolean {
        return storage.remove(id) != null
    }

    suspend fun existsById(id: Long): Boolean {
        return storage.containsKey(id)
    }

    suspend fun findByEmail(email: String): User? {
        return storage.values.find { it.email == email }
    }

    suspend fun existsByEmail(email: String): Boolean {
        return storage.values.any { it.email == email }
    }
}
