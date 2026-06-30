package com.example.webflux.repository

import com.example.webflux.domain.model.Category
import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.entity.GoodsEntity
import com.example.webflux.entity.MediaEntity
import com.example.webflux.mapper.GoodsMapper
import com.example.webflux.mapper.MediaMapper
import com.example.webflux.repository.r2dbc.MediaR2dbcRepository
import com.example.webflux.util.emitAll
import com.example.webflux.util.groupBy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

interface GoodsR2dbcRepository : CoroutineCrudRepository<GoodsEntity, String> {

    @Query("SELECT * FROM goods WHERE deleted_at IS NULL ORDER BY created_at DESC")
    fun findAllActive(): Flow<GoodsEntity>

    @Query("SELECT * FROM goods WHERE id = :id AND deleted_at IS NULL")
    suspend fun findByIdActive(id: String): GoodsEntity?

    @Query("SELECT * FROM goods WHERE category_id = :categoryId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findByCategoryId(categoryId: String): Flow<GoodsEntity>

    @Query("UPDATE goods SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)

    @Query("SELECT * FROM goods WHERE category_id = ANY(:categoryIds) AND deleted_at IS NULL ORDER BY created_at DESC")
    fun findAllByCategoryIdInActive(categoryIds: Array<String>): Flow<GoodsEntity>

    // Удалено: заменено на динамический SQL в GoodsRepository.findAllPagedOptimized()

    @Query("SELECT COUNT(*) FROM goods WHERE deleted_at IS NULL")
    suspend fun countActive(): Long

    @Query("SELECT * FROM goods WHERE id = ANY(:ids) AND deleted_at IS NULL")
    fun findByIdsIn(ids: Array<String>): Flow<GoodsEntity>
}


@Repository
class GoodsRepository(
    private val goodsR2dbcRepository: GoodsR2dbcRepository,
    private val mediaR2dbcRepository: MediaR2dbcRepository,
    private val categoryRepository: CategoryRepository,
    private val databaseClient: DatabaseClient
) {

    fun findAll(): Flow<Goods> {
        return goodsR2dbcRepository.findAllActive()
            .map { entity ->
                val media = mediaR2dbcRepository.findByGoodsId(entity.id!!)
                    .map { MediaMapper.toModel(it) }
                    .toList()
                GoodsMapper.toModel(entity, media)
            }
    }

    suspend fun findById(id: String): Goods? = coroutineScope {
        val media = async { mediaR2dbcRepository.findByGoodsId(id)
                                .map { MediaMapper.toModel(it) }
                                .toList()
                    }

        goodsR2dbcRepository.findByIdActive(id)
            ?.let { GoodsMapper.toModel(it, media.await()) }
            ?: run { media.cancel(); null }
    }

    fun findByType(type: GoodsType): Flow<Goods> = flow {
        val categories = categoryRepository.findAllByTypeActive(type)
        val goods = goodsR2dbcRepository.findAllByCategoryIdInActive(categories
                        .map(Category::id).toTypedArray()).toList()
        val medias = mediaR2dbcRepository.findByGoodsIdIn(goods.map { it.id!! }.toTypedArray())
                        .groupBy(MediaEntity::goodsId, goods.size)

        emitAll(goods.map { entity ->
            GoodsMapper.toModel(entity, medias[entity.id]
                ?.map { MediaMapper.toModel(it) } ?: emptyList() )
        })
    }

    suspend fun save(goods: Goods): Goods {
        // Медиа товара управляется отдельно через MediaService/AdminMediaController.
        // Здесь сохраняем только скалярные поля товара и возвращаем актуальные media из БД,
        // чтобы апдейт не пере-вставлял (не дублировал) строки media.
        val saved = goodsR2dbcRepository.save(GoodsMapper.toEntity(goods))

        val media = mediaR2dbcRepository.findByGoodsId(saved.id!!)
            .map { MediaMapper.toModel(it) }
            .toList()

        return GoodsMapper.toModel(saved, media)
    }

    suspend fun deleteById(id: String): Boolean {
        goodsR2dbcRepository.softDelete(id)
        mediaR2dbcRepository.softDeleteByGoodsId(id)
        return true
    }

    suspend fun findAllPaged(
        page: Int,
        size: Int,
        sortBy: String,
        sortOrder: String
    ): Pair<List<Goods>, Long> = coroutineScope {
        val offset = page * size
        val sortField = if (sortBy == "category") "created_at" else sortBy

        // Валидация параметров для защиты от SQL injection
        val validSortFields = setOf("name", "price", "created_at")
        val validSortOrders = setOf("ASC", "DESC")

        val safeSortField = if (sortField in validSortFields) sortField else "created_at"
        val safeSortOrder = if (sortOrder.uppercase() in validSortOrders) sortOrder.uppercase() else "DESC"

        // Оптимизированный динамический SQL запрос (вместо множественных CASE)
        val sql = """
            SELECT * FROM goods
            WHERE deleted_at IS NULL
            ORDER BY $safeSortField $safeSortOrder
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        // Загрузить entities с пагинацией через DatabaseClient
        val entities = async {
            databaseClient.sql(sql)
                .bind("limit", size)
                .bind("offset", offset)
                .map { row, _ ->
                    GoodsEntity(
                        id = row.get("id", String::class.java),
                        name = row.get("name", String::class.java)!!,
                        description = row.get("description", String::class.java)!!,
                        price = row.get("price", java.math.BigDecimal::class.java)!!,
                        categoryId = row.get("category_id", String::class.java)!!,
                        difficulty = row.get("difficulty", String::class.java)!!,
                        duration = row.get("duration", Integer::class.java)?.toInt(),
                        videoUrl = row.get("video_url", String::class.java),
                        detailedDescription = row.get("detailed_description", String::class.java),
                        careInstructions = row.get("care_instructions", String::class.java),
                        createdAt = row.get("created_at", java.time.OffsetDateTime::class.java)!!,
                        updatedAt = row.get("updated_at", java.time.OffsetDateTime::class.java)!!,
                        deletedAt = row.get("deleted_at", java.time.OffsetDateTime::class.java)
                    )
                }
                .all()
                .asFlow()
                .toList()
        }

        val totalElements = async { goodsR2dbcRepository.countActive() }

        // Batch загрузка media для всех товаров на странице
        val goodsIds = entities.await().map { it.id!! }
        val mediaMap = if (goodsIds.isNotEmpty()) {
            mediaR2dbcRepository.findByGoodsIdIn(goodsIds.toTypedArray())
                .groupBy(MediaEntity::goodsId, goodsIds.size)
        } else {
            emptyMap()
        }

        var goods = entities.await().map { entity ->
            GoodsMapper.toModel(
                entity,
                mediaMap[entity.id]?.map { MediaMapper.toModel(it) } ?: emptyList()
            )
        }

        // Применить сортировку по категории (in-memory) если нужно
        if (sortBy == "category") {
            val categories = categoryRepository.findAll()
                .fold(mutableMapOf<String, String>()) { acc, cat ->
                    acc[cat.id] = cat.name
                    acc
                }

            goods = if (sortOrder.uppercase() == "ASC") {
                goods.sortedBy { categories[it.categoryId] ?: "" }
            } else {
                goods.sortedByDescending { categories[it.categoryId] ?: "" }
            }
        }

        goods to totalElements.await()
    }

    fun findByIdsIn(ids: Collection<String>): Flow<Goods> =
        goodsR2dbcRepository.findByIdsIn(ids.toTypedArray()).map { GoodsMapper.toModel(it, emptyList()) }

    /**
     * Серверные поиск + сортировка + пагинация по типу товара (для публичного каталога).
     * Фильтрует по категориям заданного типа, опционально по тексту (name/description), сортирует и
     * возвращает одну страницу + общее количество. Сделано на DatabaseClient по образцу findAllPaged.
     */
    suspend fun findByTypePaged(
        type: GoodsType,
        query: String?,
        sortBy: String,
        sortOrder: String,
        page: Int,
        size: Int
    ): Pair<List<Goods>, Long> = coroutineScope {
        val categoryIds = categoryRepository.findAllByTypeActive(type).map { it.id }.toTypedArray()
        if (categoryIds.isEmpty()) return@coroutineScope emptyList<Goods>() to 0L

        val offset = page * size

        // Валидация параметров сортировки — защита от SQL injection
        val safeSortField = if (sortBy in setOf("name", "price", "created_at")) sortBy else "created_at"
        val safeSortOrder = if (sortOrder.uppercase() in setOf("ASC", "DESC")) sortOrder.uppercase() else "DESC"

        val trimmed = query?.trim()
        val hasSearch = !trimmed.isNullOrEmpty()
        val searchClause = if (hasSearch) " AND (name ILIKE :pattern OR description ILIKE :pattern)" else ""
        val baseWhere = "WHERE deleted_at IS NULL AND category_id = ANY(:categoryIds)$searchClause"

        val sql = "SELECT * FROM goods $baseWhere ORDER BY $safeSortField $safeSortOrder LIMIT :limit OFFSET :offset"
        val countSql = "SELECT COUNT(*) FROM goods $baseWhere"

        val entities = async {
            var spec = databaseClient.sql(sql)
                .bind("categoryIds", categoryIds)
                .bind("limit", size)
                .bind("offset", offset)
            if (hasSearch) spec = spec.bind("pattern", "%$trimmed%")
            spec.map { row, _ ->
                GoodsEntity(
                    id = row.get("id", String::class.java),
                    name = row.get("name", String::class.java)!!,
                    description = row.get("description", String::class.java)!!,
                    price = row.get("price", java.math.BigDecimal::class.java)!!,
                    categoryId = row.get("category_id", String::class.java)!!,
                    difficulty = row.get("difficulty", String::class.java)!!,
                    duration = row.get("duration", Integer::class.java)?.toInt(),
                    videoUrl = row.get("video_url", String::class.java),
                    detailedDescription = row.get("detailed_description", String::class.java),
                    careInstructions = row.get("care_instructions", String::class.java),
                    createdAt = row.get("created_at", java.time.OffsetDateTime::class.java)!!,
                    updatedAt = row.get("updated_at", java.time.OffsetDateTime::class.java)!!,
                    deletedAt = row.get("deleted_at", java.time.OffsetDateTime::class.java)
                )
            }.all().asFlow().toList()
        }

        val total = async {
            var spec = databaseClient.sql(countSql).bind("categoryIds", categoryIds)
            if (hasSearch) spec = spec.bind("pattern", "%$trimmed%")
            spec.map { row, _ -> row.get(0, java.lang.Long::class.java)?.toLong() ?: 0L }
                .all().asFlow().toList().firstOrNull() ?: 0L
        }

        val entitiesList = entities.await()
        val goodsIds = entitiesList.map { it.id!! }
        val mediaMap = if (goodsIds.isNotEmpty()) {
            mediaR2dbcRepository.findByGoodsIdIn(goodsIds.toTypedArray())
                .groupBy(MediaEntity::goodsId, goodsIds.size)
        } else emptyMap()

        val goods = entitiesList.map { entity ->
            GoodsMapper.toModel(entity, mediaMap[entity.id]?.map { MediaMapper.toModel(it) } ?: emptyList())
        }

        goods to total.await()
    }
}
