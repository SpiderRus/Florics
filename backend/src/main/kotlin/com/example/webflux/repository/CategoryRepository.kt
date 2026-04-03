package com.example.webflux.repository

import com.example.webflux.domain.model.Category
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.mapper.CategoryMapper
import com.example.webflux.repository.r2dbc.CategoryR2dbcRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap


@Repository
class CategoryRepository(
    private val categoryR2dbcRepository: CategoryR2dbcRepository
) {
    private val cache = ConcurrentHashMap<String, Category>()
    private val reloadScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var active = true

    @PostConstruct
    fun init() {
        reloadScope.launch {
            delay(10 * 1_000)

            while (active) {
                try {
                    reloadCache()
                } catch (th: Throwable) {
                    logger.error(th.message, th)
                }

                delay(5 * 60 * 1_000)
            }
        }
    }

    @PreDestroy
    fun destroy() {
        active = false
        reloadScope.cancel()
    }

    private suspend fun reloadCache() {
        val categories = categoryR2dbcRepository.findAll().toList()

        cache.clear()
        categories.forEach { category -> cache[category.id.toString()] = CategoryMapper.toModel(category) }
    }

    /**
     * Получить все активные категории с кэшированием
     * Кэш живет 5 минут (TTL в CacheConfig)
     */
    fun findAll(): List<Category> = cache.values.filter { it.deletedAt == null }.toList()

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
    fun findAllByTypeActive(type: GoodsType): List<Category> =
        cache.values.filter { it.deletedAt == null && it.type == type }.toList()

    private companion object {
        private val logger = LoggerFactory.getLogger(CategoryRepository::class.java)
    }
}
