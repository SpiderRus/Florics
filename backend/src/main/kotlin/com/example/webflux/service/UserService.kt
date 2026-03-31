package com.example.webflux.service

import com.example.webflux.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

@Service
class UserService {

    // BCrypt hash для "password123"
    private val users = mutableMapOf(
        1L to User(1, "Alice", "alice@example.com", "\$2a\$10\$GLV9oczk1dvKX9vJ2gHlhOn41DInakVQiRSDzJehbIE7Uqr2VLZIy", setOf("USER")),
        2L to User(2, "Bob", "bob@example.com", "\$2a\$10\$GLV9oczk1dvKX9vJ2gHlhOn41DInakVQiRSDzJehbIE7Uqr2VLZIy", setOf("USER")),
        3L to User(3, "Admin", "admin@example.com", "\$2a\$10\$GLV9oczk1dvKX9vJ2gHlhOn41DInakVQiRSDzJehbIE7Uqr2VLZIy", setOf("USER", "ADMIN"))
    )

    suspend fun getUserById(id: Long): User? {
        return users[id]
    }

    suspend fun createUser(user: User): User {
        users[user.id] = user
        return user
    }

    fun getAllUsers(): Flow<User> = flow {
        users.values.forEach { user ->
            emit(user)
        }
    }

    suspend fun updateUser(id: Long, user: User): User? {
        return if (users.containsKey(id)) {
            users[id] = user.copy(id = id)
            users[id]
        } else
            null
    }

    suspend fun deleteUser(id: Long): Boolean {
        return users.remove(id) != null
    }
}
