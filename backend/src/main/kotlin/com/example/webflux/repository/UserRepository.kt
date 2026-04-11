package com.example.webflux.repository

import com.example.webflux.domain.model.User
import com.example.webflux.entity.UserEntity
import com.example.webflux.mapper.UserMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

interface UserR2dbcRepository : CoroutineCrudRepository<UserEntity, String> {

    @Query("SELECT * FROM users WHERE email = :email AND deleted_at IS NULL")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE deleted_at IS NULL")
    fun findAllActive(): Flow<UserEntity>

    @Query("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}


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
        try {
            val savedEntity = userR2dbcRepository.save(UserMapper.toEntity(user))
            log.info("Entity saved to database: id=${savedEntity.id}, email=${savedEntity.email}")

            return UserMapper.toModel(savedEntity)
        } catch (e: Exception) {
            log.error("Failed to save user: ${user.email}", e)
            throw e
        }
    }

    fun findAll(): Flow<User> = userR2dbcRepository.findAllActive().map { UserMapper.toModel(it) }

    suspend fun deleteById(id: String): Boolean = userR2dbcRepository.softDelete(id).let { true }

    suspend fun existsById(id: String): Boolean =
        userR2dbcRepository.findById(id)?.let { it.deletedAt == null } ?: false

    suspend fun findByEmail(email: String): User? =
        userR2dbcRepository.findByEmail(email)?.let { UserMapper.toModel(it) }

    suspend fun existsByEmail(email: String): Boolean = userR2dbcRepository.findByEmail(email) != null

    private companion object {
        val log = LoggerFactory.getLogger(UserRepository::class.java)!!
    }
}
