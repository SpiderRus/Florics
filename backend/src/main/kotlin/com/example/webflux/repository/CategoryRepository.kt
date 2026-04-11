package com.example.webflux.repository

import com.example.webflux.domain.model.Category
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.entity.CategoryEntity
import com.example.webflux.mapper.CategoryMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds


interface CategoryR2dbcRepository : CoroutineCrudRepository<CategoryEntity, String> {
    @Query("SELECT * FROM categories WHERE id = :id AND deleted_at IS NULL")
    suspend fun findByIdActive(id: String): CategoryEntity?

    @Query("UPDATE categories SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)
}


@Repository
class CategoryRepository(
    private val categoryR2dbcRepository: CategoryR2dbcRepository,
    @param:Qualifier("asyncJobScope") private val reloadScope: CoroutineScope
) {
    private val cache = ConcurrentHashMap<String, Category>()

    @Volatile private var active = true

    @PostConstruct
    fun init() {
        reloadScope.launch {
            while (active) {
                delay(RELOAD_DURATION_MS)

                reloadCache()
            }
        }
    }

    @PreDestroy
    fun destroy() {
        active = false
        reloadScope.cancel()
    }

    private suspend fun reloadCache() {
        try {
            cache.clear()
            categoryR2dbcRepository.findAll().collect { category ->
                cache[category.id!!] = CategoryMapper.toModel(category)
            }
        } catch (th: Throwable) {
            logger.error(th.message, th)
        }
    }

    /**
     * Получить все активные категории с кэшированием
     * Кэш живет 5 минут (TTL в CacheConfig)
     */
    suspend fun findAll(): List<Category> {
        if (cache.isEmpty())
            reloadCache()

        return cache.values.filter { it.deletedAt == null }.toList()
    }

    /**
     * Получить категорию по ID с кэшированием
     */
    suspend fun findById(id: String): Category? {
        cache[id] ?.let { return it }

        return categoryR2dbcRepository.findByIdActive(id)?.let {
                reloadScope.launch { reloadCache() }
                CategoryMapper.toModel(it)
            }
    }

    /**
     * Получить категории по типу с кэшированием
     */
    suspend fun findAllByTypeActive(type: GoodsType): List<Category> =
        findAll().filter { it.type == type }.toList()

    fun invalidateCache() = reloadScope.launch { reloadCache() }

    private companion object {
        val RELOAD_DURATION_MS = (5 * 60 * 1_000).milliseconds

        private val logger = LoggerFactory.getLogger(CategoryRepository::class.java)
    }
}
