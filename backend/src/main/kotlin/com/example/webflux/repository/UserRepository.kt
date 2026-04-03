package com.example.webflux.repository

import com.example.webflux.domain.model.User
import com.example.webflux.mapper.UserMapper
import com.example.webflux.repository.r2dbc.UserR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepository(
    private val userR2dbcRepository: UserR2dbcRepository
) {

    suspend fun findById(id: String): User? {
        val entity = userR2dbcRepository.findById(id) ?: return null
        if (entity.deletedAt != null) return null
        return UserMapper.toModel(entity)
    }

    suspend fun save(user: User): User =
        UserMapper.toModel(userR2dbcRepository.save(UserMapper.toEntity(user)))

    fun findAll(): Flow<User> {
        return userR2dbcRepository.findAllActive()
            .map { UserMapper.toModel(it) }
    }

    suspend fun deleteById(id: String): Boolean {
        if (!existsById(id)) return false
        userR2dbcRepository.softDelete(id)
        return true
    }

    suspend fun existsById(id: String): Boolean {
        val entity = userR2dbcRepository.findById(id) ?: return false
        return entity.deletedAt == null
    }

    suspend fun findByEmail(email: String): User? {
        val entity = userR2dbcRepository.findByEmail(email) ?: return null
        return UserMapper.toModel(entity)
    }

    suspend fun existsByEmail(email: String): Boolean {
        return userR2dbcRepository.findByEmail(email) != null
    }
}
