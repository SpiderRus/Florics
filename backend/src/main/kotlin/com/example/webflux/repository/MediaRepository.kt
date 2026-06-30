package com.example.webflux.repository

import com.example.webflux.entity.MediaEntity
import com.example.webflux.repository.r2dbc.MediaR2dbcRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.util.UUID

/** Содержимое фото из БД для отдачи байтов. */
data class MediaContent(val content: ByteArray, val contentType: String)

/**
 * Владелец медиа товара: строки таблицы media + бинарное содержимое media_content.
 * Запись media_content и вставка media с заранее известным id делаются через DatabaseClient,
 * чтобы обойти семантику CrudRepository.save для не-генерируемых PK и маппинг bytea.
 */
@Repository
class MediaRepository(
    private val mediaR2dbcRepository: MediaR2dbcRepository,
    private val databaseClient: DatabaseClient
) {
    suspend fun findByGoodsId(goodsId: String): List<MediaEntity> =
        mediaR2dbcRepository.findByGoodsId(goodsId).toList()

    suspend fun nextOrder(goodsId: String): Int {
        val max = databaseClient.sql(
            "SELECT COALESCE(MAX(display_order), -1) FROM media WHERE goods_id = :gid AND deleted_at IS NULL"
        ).bind("gid", goodsId)
            .map { row, _ -> (row.get(0, java.lang.Integer::class.java) ?: -1).toInt() }
            .one().awaitSingle()
        return max + 1
    }

    suspend fun insertDbPhoto(goodsId: String, bytes: ByteArray, contentType: String, order: Int): MediaEntity {
        val id = UUID.randomUUID().toString()
        val url = "/api/media/$id"
        databaseClient.sql(
            "INSERT INTO media (id, goods_id, type, url, display_order) VALUES (:id, :gid, 'IMAGE', :url, :ord)"
        ).bind("id", id).bind("gid", goodsId).bind("url", url).bind("ord", order)
            .fetch().rowsUpdated().awaitSingle()
        databaseClient.sql(
            "INSERT INTO media_content (media_id, content, content_type, size_bytes) " +
                "VALUES (:mid, :content, :ct, :sz)"
        ).bind("mid", id).bind("content", bytes).bind("ct", contentType).bind("sz", bytes.size)
            .fetch().rowsUpdated().awaitSingle()
        return MediaEntity(id = id, goodsId = goodsId, type = "IMAGE", url = url, displayOrder = order)
    }

    suspend fun insertExternal(goodsId: String, url: String, order: Int): MediaEntity =
        mediaR2dbcRepository.save(
            MediaEntity(id = null, goodsId = goodsId, type = "IMAGE", url = url, displayOrder = order)
        )

    suspend fun updateOrder(mediaId: String, order: Int) {
        databaseClient.sql("UPDATE media SET display_order = :ord WHERE id = :id AND deleted_at IS NULL")
            .bind("ord", order).bind("id", mediaId)
            .fetch().rowsUpdated().awaitSingle()
    }

    suspend fun softDelete(mediaId: String) = mediaR2dbcRepository.softDelete(mediaId)

    suspend fun loadContent(mediaId: String): MediaContent? =
        databaseClient.sql("SELECT content, content_type FROM media_content WHERE media_id = :id")
            .bind("id", mediaId)
            .map { row, _ ->
                MediaContent(
                    content = row.get("content", ByteArray::class.java)!!,
                    contentType = row.get("content_type", String::class.java)!!
                )
            }
            .one().awaitFirstOrNull()
}
