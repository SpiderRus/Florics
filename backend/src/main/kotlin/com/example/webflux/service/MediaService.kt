package com.example.webflux.service

import com.example.webflux.controller.model.AdminMediaDto
import com.example.webflux.controller.model.MediaReconcileItemDto
import com.example.webflux.entity.MediaEntity
import com.example.webflux.repository.MediaContent
import com.example.webflux.repository.MediaRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class MediaService(
    private val mediaRepository: MediaRepository
) {
    suspend fun listByGoods(goodsId: String): List<AdminMediaDto> =
        mediaRepository.findByGoodsId(goodsId).map { it.toAdminDto() }

    suspend fun uploadPhoto(goodsId: String, file: FilePart): AdminMediaDto {
        val bytes = file.readBytes()
        val contentType = file.headers().contentType?.toString() ?: "image/jpeg"
        val order = mediaRepository.nextOrder(goodsId)
        return mediaRepository.insertDbPhoto(goodsId, bytes, contentType, order).toAdminDto()
    }

    /**
     * Привести набор медиа товара к присланному упорядоченному списку:
     * удалить отсутствующие, вставить новые внешние, обновить порядок существующих.
     */
    suspend fun reconcile(goodsId: String, items: List<MediaReconcileItemDto>): List<AdminMediaDto> {
        val existing = mediaRepository.findByGoodsId(goodsId)
        val existingIds = existing.mapNotNull { it.id }.toSet()
        val keptIds = items.mapNotNull { it.id }.toSet()

        (existingIds - keptIds).forEach { mediaRepository.softDelete(it) }

        items.forEach { item ->
            when {
                item.id != null && item.id in existingIds ->
                    mediaRepository.updateOrder(item.id, item.order)
                item.id == null && !item.url.isNullOrBlank() ->
                    mediaRepository.insertExternal(goodsId, item.url.trim(), item.order)
                // item.id, которого нет среди существующих — игнорируем (защита от рассинхрона)
            }
        }
        return listByGoods(goodsId)
    }

    suspend fun loadContent(mediaId: String): MediaContent? = mediaRepository.loadContent(mediaId)

    private fun MediaEntity.toAdminDto() =
        AdminMediaDto(id = id!!, type = type, url = url, order = displayOrder)

    private suspend fun FilePart.readBytes(): ByteArray {
        val buffer = DataBufferUtils.join(content()).awaitSingle()
        val bytes = ByteArray(buffer.readableByteCount())
        buffer.read(bytes)
        DataBufferUtils.release(buffer)
        return bytes
    }
}
