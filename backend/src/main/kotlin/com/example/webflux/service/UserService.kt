package com.example.webflux.service

import com.example.webflux.domain.model.User
import com.example.webflux.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    suspend fun getUserById(id: String): User? {
        return userRepository.findById(id)
    }

    suspend fun createUser(user: User): User {
        return userRepository.save(user)
    }

    fun getAllUsers(): Flow<User> {
        return userRepository.findAll()
    }

    suspend fun updateUser(id: String, user: User): User? {
        if (!userRepository.existsById(id)) return null
        return userRepository.save(user.copy(id = id))
    }

    suspend fun deleteUser(id: String): Boolean {
        return userRepository.deleteById(id)
    }
}
