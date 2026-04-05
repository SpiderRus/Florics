package com.example.webflux.service.document.dto

/**
 * DTO для запроса загрузки документа в AI Agent
 *
 * Используется для отправки документов в систему AI Agent с последующей векторизацией.
 * Документ будет разбит на чанки, для каждого создан эмбеддинг и сохранён в pgvector.
 *
 * @property filename Имя файла документа (обязательное)
 * @property content Текстовое содержимое документа (обязательное)
 * @property metadata Дополнительные метаданные документа (тип, автор, дата и т.д.)
 *                    Будут сохранены в JSON поле vector_store таблицы
 */
data class DocumentUploadRequest(
    val filename: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)
