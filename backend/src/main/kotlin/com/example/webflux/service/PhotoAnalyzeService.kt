package com.example.webflux.service

import com.example.webflux.repository.MediaRepository
import com.example.webflux.service.aibot.AiBotServiceException
import com.example.webflux.service.aibot.AiBotTimeoutException
import com.example.webflux.service.aibot.dto.ImageBytes
import com.example.webflux.service.aibot.dto.PlantCardDto
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Резолвит фотографии товара (blob-файлы + db по mediaId + внешние по url) в байты и
 * отправляет их боту photo-analyzer. db-байты читаются из media_content, внешние —
 * server-side fetch (CORS не действует). Неудачные внешние — пропускаются (best-effort).
 */
@Service
class PhotoAnalyzeService(
    private val mediaRepository: MediaRepository,
    private val aiBotService: AiBotService,
    private val objectMapper: ObjectMapper
) {
    // Отдельный клиент с поднятым лимитом — внешняя картинка может быть > 256 КБ (дефолт кодека).
    private val externalClient = WebClient.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()

    suspend fun analyze(files: List<FilePart>, mediaIdsJson: String?, urlsJson: String?): PlantCardDto {
        val images = mutableListOf<ImageBytes>()

        // 1) Новые blob-файлы — байты от клиента
        for (f in files) {
            val ct = f.headers().contentType?.toString() ?: "image/jpeg"
            images += ImageBytes(f.readBytesJoined(), ct)
        }
        // 2) Сохранённые db-фото — байты из media_content
        parseStringArray(mediaIdsJson).forEach { id ->
            mediaRepository.loadContent(id)?.let { images += ImageBytes(it.content, it.contentType) }
        }
        // 3) Внешние URL — server-side fetch
        parseStringArray(urlsJson).forEach { url ->
            fetchExternal(url)?.let { images += it }
        }

        if (images.isEmpty()) {
            return PlantCardDto(error = "Не удалось получить изображения для анализа")
        }

        return try {
            aiBotService.analyzePhotos(images)
        } catch (e: AiBotServiceException) {
            logger.error("Photo-analyzer call failed", e)
            PlantCardDto(error = "AI-сервис недоступен, попробуйте позже")
        } catch (e: AiBotTimeoutException) {
            logger.error("Photo-analyzer timed out", e)
            PlantCardDto(error = "Анализ занял слишком долго, попробуйте ещё раз")
        }
    }

    /**
     * Заполнить карточку товара по названию: шлём текст названия боту photo-analyzer (без картинок).
     * Перехват ошибок апстрима — как в [analyze].
     */
    suspend fun analyzeByName(name: String): PlantCardDto {
        if (name.isBlank()) {
            return PlantCardDto(error = "Введите название для анализа")
        }
        return try {
            aiBotService.analyzeByName(name.trim())
        } catch (e: AiBotServiceException) {
            logger.error("Photo-analyzer (by name) call failed", e)
            PlantCardDto(error = "AI-сервис недоступен, попробуйте позже")
        } catch (e: AiBotTimeoutException) {
            logger.error("Photo-analyzer (by name) timed out", e)
            PlantCardDto(error = "Анализ занял слишком долго, попробуйте ещё раз")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            (objectMapper.readValue(json, List::class.java) as List<Any?>).mapNotNull { it as? String }
        } catch (e: Exception) {
            logger.warn("Failed to parse refs JSON: {}", json)
            emptyList()
        }
    }

    private suspend fun fetchExternal(url: String): ImageBytes? = try {
        val entity = externalClient.get().uri(url).retrieve()
            .toEntity(ByteArray::class.java)
            .timeout(Duration.ofSeconds(15))
            .awaitFirstOrNull()
        val body = entity?.body
        if (body == null || body.isEmpty()) null
        else ImageBytes(body, entity.headers.contentType?.toString() ?: "image/jpeg")
    } catch (e: Exception) {
        logger.warn("Failed to fetch external image {}: {}", url, e.message)
        null
    }

    private suspend fun FilePart.readBytesJoined(): ByteArray {
        val buffer = DataBufferUtils.join(content()).awaitSingle()
        val bytes = ByteArray(buffer.readableByteCount())
        buffer.read(bytes)
        DataBufferUtils.release(buffer)
        return bytes
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(PhotoAnalyzeService::class.java)
    }
}
