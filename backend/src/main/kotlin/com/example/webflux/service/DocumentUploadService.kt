package com.example.webflux.service

import com.example.webflux.service.document.DocumentServiceException
import com.example.webflux.service.document.DocumentUploadTimeoutException
import com.example.webflux.service.document.dto.DocumentResponse
import com.example.webflux.service.document.dto.DocumentUploadRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import java.util.concurrent.TimeoutException

/**
 * Реактивный сервис для загрузки документов в AI Agent
 *
 * Предоставляет suspend функции для загрузки текстовых документов в систему AI Agent
 * с автоматической векторизацией и подготовкой для RAG (Retrieval-Augmented Generation).
 *
 * Процесс обработки документа в AI Agent:
 * 1. Документ разбивается на chunks (фрагменты) через TokenTextSplitter
 * 2. Для каждого chunk создаётся векторное представление (embedding) через Ollama (nomic-embed-text, 768 измерений)
 * 3. Сохраняется в pgvector базу для семантического поиска
 * 4. Документ становится доступен для RAG в Chat API с useRag=true
 *
 * @property webClient WebClient для взаимодействия с AI Agent API
 */
@Service
class DocumentUploadService(
    @Qualifier("aiAgentWebClient") private val webClient: WebClient
) {
    /**
     * Загрузить документ в AI Agent для векторизации
     *
     * Отправляет текстовый документ в AI Agent, который автоматически:
     * - Разбивает текст на chunks
     * - Создаёт embeddings через Ollama
     * - Сохраняет в pgvector для семантического поиска
     *
     * После успешной загрузки документ доступен для использования в RAG queries
     * через Chat API с параметром useRag=true.
     *
     * @param filename Имя файла документа
     * @param content Текстовое содержимое документа
     * @param metadata Дополнительные метаданные (тип, автор, дата и т.д.)
     * @return Информация о загруженном документе (id, filename, chunksCount, uploadedAt)
     * @throws DocumentServiceException при ошибке связи с AI Agent или ошибках обработки
     * @throws DocumentUploadTimeoutException при timeout запроса
     */
    suspend fun uploadDocument(
        filename: String,
        content: String,
        metadata: Map<String, String> = emptyMap()
    ): DocumentResponse {
        logger.debug("Uploading document: {} ({} chars)", filename, content.length)

        val request = DocumentUploadRequest(
            filename = filename,
            content = content,
            metadata = metadata
        )

        return try {
            webClient.post()
                .uri("/documents")
                .bodyValue(request)
                .retrieve()
                .onStatus({ it.is4xxClientError }, errorHandler4xx)
                .onStatus({ it.is5xxServerError }, errorHandler5xx)
                .awaitBody<DocumentResponse>()
                .also {
                    logger.debug("Document uploaded successfully: {} (id: {}, chunks: {})",
                        filename, it.id, it.chunksCount)
                }
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI Agent service for document upload", e)
            throw DocumentServiceException("Failed to connect to AI Agent service", e)
        } catch (e: TimeoutException) {
            logger.error("Document upload request timed out", e)
            throw DocumentUploadTimeoutException("Document upload request timed out", e)
        }
    }

    /**
     * Загрузить документ с автоматическим заполнением метаданных
     *
     * Удобный метод для загрузки документа с типичными метаданными.
     *
     * @param filename Имя файла документа
     * @param content Текстовое содержимое документа
     * @param documentType Тип документа (например: "product_description", "faq", "article")
     * @param author Автор документа
     * @return Информация о загруженном документе
     * @throws DocumentServiceException при ошибке связи с AI Agent или ошибках обработки
     * @throws DocumentUploadTimeoutException при timeout запроса
     */
    suspend fun uploadDocument(
        filename: String,
        content: String,
        documentType: String? = null,
        author: String? = null
    ): DocumentResponse {
        val metadata = buildMap {
            documentType?.let { put("type", it) }
            author?.let { put("author", it) }
        }
        return uploadDocument(filename, content, metadata)
    }

    /**
     * Error handler для 4xx ошибок
     */
    private val errorHandler4xx: (ClientResponse) -> Mono<out Throwable> = { response ->
        response.bodyToMono<String>().map { body ->
            when (response.statusCode().value()) {
                404 -> DocumentServiceException("Document endpoint not found in AI Agent")
                400 -> DocumentServiceException("Invalid document data: $body")
                413 -> DocumentServiceException("Document too large: $body")
                else -> DocumentServiceException("AI Agent client error (${response.statusCode()}): $body")
            }
        }.defaultIfEmpty(DocumentServiceException("AI Agent client error: ${response.statusCode()}"))
    }

    /**
     * Error handler для 5xx ошибок
     */
    private val errorHandler5xx: (ClientResponse) -> Mono<out Throwable> = { response ->
        response.bodyToMono<String>().map { body ->
            DocumentServiceException("AI Agent server error (${response.statusCode()}): $body")
        }.defaultIfEmpty(DocumentServiceException("AI Agent server error: ${response.statusCode()}"))
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DocumentUploadService::class.java)
    }
}
