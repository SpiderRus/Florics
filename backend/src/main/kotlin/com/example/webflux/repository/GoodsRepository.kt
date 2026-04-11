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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
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

    @Query("""
        SELECT * FROM goods
        WHERE deleted_at IS NULL
        ORDER BY
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN name END DESC,
            CASE WHEN :sortBy = 'price' AND :sortOrder = 'ASC' THEN price END ASC,
            CASE WHEN :sortBy = 'price' AND :sortOrder = 'DESC' THEN price END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN created_at END DESC
        LIMIT :limit OFFSET :offset
    """)
    fun findAllActivePaged(
        sortBy: String,
        sortOrder: String,
        limit: Int,
        offset: Int
    ): Flow<GoodsEntity>

    @Query("SELECT COUNT(*) FROM goods WHERE deleted_at IS NULL")
    suspend fun countActive(): Long

    @Query("SELECT * FROM goods WHERE id = ANY(:ids) AND deleted_at IS NULL")
    fun findByIdsIn(ids: Array<String>): Flow<GoodsEntity>
}


@Repository
class GoodsRepository(
    private val goodsR2dbcRepository: GoodsR2dbcRepository,
    private val mediaR2dbcRepository: MediaR2dbcRepository,
    private val categoryRepository: CategoryRepository
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

    suspend fun findById(id: String): Goods? {
        val entity = goodsR2dbcRepository.findByIdActive(id) ?: return null
        val media = mediaR2dbcRepository.findByGoodsId(id)
            .map { MediaMapper.toModel(it) }
            .toList()

        return GoodsMapper.toModel(entity, media)
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
        val entity = GoodsMapper.toEntity(goods)
        val saved = goodsR2dbcRepository.save(entity)

        // Save media - дожидаемся сохранения каждого элемента
        goods.media.forEach { media ->
            mediaR2dbcRepository.save(MediaMapper.toEntity(media, saved.id!!)).let { }
        }

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
    ): Pair<List<Goods>, Long> {
        val offset = page * size
        val sortField = if (sortBy == "category") "created_at" else sortBy

        // Загрузить entities с пагинацией
        val entities = goodsR2dbcRepository.findAllActivePaged(
            sortBy = sortField,
            sortOrder = sortOrder.uppercase(),
            limit = size,
            offset = offset
        ).toList()

        val totalElements = goodsR2dbcRepository.countActive()

        // Batch загрузка media для всех товаров на странице
        val goodsIds = entities.map { it.id!! }
        val mediaMap = if (goodsIds.isNotEmpty()) {
            mediaR2dbcRepository.findByGoodsIdIn(goodsIds.toTypedArray())
                .groupBy(MediaEntity::goodsId, goodsIds.size)
        } else {
            emptyMap()
        }

        var goods = entities.map { entity ->
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

        return goods to totalElements
    }

    fun findByIdsIn(ids: Collection<String>): Flow<Goods> =
        goodsR2dbcRepository.findByIdsIn(ids.toTypedArray()).map { GoodsMapper.toModel(it, emptyList()) }
}
