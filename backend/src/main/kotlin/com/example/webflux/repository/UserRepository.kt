package com.example.webflux.repository

import com.example.webflux.domain.model.User
import com.example.webflux.mapper.UserMapper
import com.example.webflux.repository.r2dbc.UserR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
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

    suspend fun save(user: User): User {
        log.info("UserRepository.save() called for user: id=${user.id}, email=${user.email}")
        try {
            val entity = UserMapper.toEntity(user)
            log.debug("Converted to entity: id={}, email={}, roles={}", entity.id, entity.email, entity.roles)

            val savedEntity = userR2dbcRepository.save(entity)
            log.info("Entity saved to database: id=${savedEntity.id}, email=${savedEntity.email}")

            val savedUser = UserMapper.toModel(savedEntity)
            log.info("Converted back to model: id=${savedUser.id}, email=${savedUser.email}")

            return savedUser
        } catch (e: Exception) {
            log.error("Failed to save user: ${user.email}", e)
            throw e
        }
    }

    fun findAll(): Flow<User> = userR2dbcRepository.findAllActive().map { UserMapper.toModel(it) }

    suspend fun deleteById(id: String): Boolean =
        if (!existsById(id))
            false
        else
            userR2dbcRepository.softDelete(id).let { true }

    suspend fun existsById(id: String): Boolean =
        userR2dbcRepository.findById(id)?.let { it.deletedAt == null } ?: false

    suspend fun findByEmail(email: String): User? =
        userR2dbcRepository.findByEmail(email)?.let { UserMapper.toModel(it) }

    suspend fun existsByEmail(email: String): Boolean = userR2dbcRepository.findByEmail(email) != null

    private companion object {
        val log = LoggerFactory.getLogger(UserRepository::class.java)!!
    }
}
