package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.MediaEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MediaR2dbcRepository : CoroutineCrudRepository<MediaEntity, String> {

    @Query("SELECT * FROM media WHERE goods_id = :goodsId AND deleted_at IS NULL ORDER BY display_order")
    fun findByGoodsId(goodsId: String): Flow<MediaEntity>

    @Query("SELECT * FROM media WHERE goods_id = ANY(:goodsIds) AND deleted_at IS NULL ORDER BY display_order")
    fun findByGoodsIdIn(goodsIds: Array<String>): Flow<MediaEntity>

    fun findByGoodsIdIn(goodsIds: Collection<String>): Flow<MediaEntity> =
        findByGoodsIdIn(goodsIds.toTypedArray())

    @Query("UPDATE media SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String)

    @Query("UPDATE media SET deleted_at = CURRENT_TIMESTAMP WHERE goods_id = :goodsId AND deleted_at IS NULL")
    suspend fun softDeleteByGoodsId(goodsId: String)
}
