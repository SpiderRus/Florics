package com.example.webflux.repository

import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.entity.MediaEntity
import com.example.webflux.mapper.GoodsMapper
import com.example.webflux.mapper.MediaMapper
import com.example.webflux.repository.r2dbc.GoodsR2dbcRepository
import com.example.webflux.repository.r2dbc.MediaR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository

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
        val goods = goodsR2dbcRepository.findAllByCategoryIdInActive(categories.map { it.id }).toList()
        val medias = mediaR2dbcRepository.findByGoodsIdIn(goods.map { it.id!! })
            .fold(mutableMapOf<String, MutableList<MediaEntity>>()) { acc, value ->
                acc.apply { getOrPut(value.goodsId) { mutableListOf() }.add(value) }
            }

        goods.forEach { entity ->
            emit(GoodsMapper.toModel(entity, medias[entity.id]
                    ?.map { MediaMapper.toModel(it) } ?: emptyList() ))
        }
    }

    suspend fun save(goods: Goods): Goods {
        val entity = GoodsMapper.toEntity(goods)
        val saved = goodsR2dbcRepository.save(entity)

        // Save media
        goods.media.forEach { media ->
            mediaR2dbcRepository.save(MediaMapper.toEntity(media, saved.id!!))
        }

        val media = mediaR2dbcRepository.findByGoodsId(saved.id!!)
            .map { MediaMapper.toModel(it) }
            .toList()

        return GoodsMapper.toModel(saved, media)
    }

    suspend fun deleteById(id: String): Boolean {
        if (goodsR2dbcRepository.findByIdActive(id) == null) return false

        goodsR2dbcRepository.softDelete(id)
        mediaR2dbcRepository.softDeleteByGoodsId(id)
        return true
    }
}
