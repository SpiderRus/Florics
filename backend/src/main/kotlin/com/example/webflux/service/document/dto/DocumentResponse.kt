package com.example.webflux.service.document.dto

import java.time.LocalDateTime

/**
 * DTO для ответа после загрузки документа из AI Agent
 *
 * Возвращается после успешной обработки и векторизации документа.
 *
 * @property id Уникальный идентификатор документа (UUID)
 * @property filename Имя файла документа
 * @property chunksCount Количество чанков, на которые был разбит документ
 *                       0 означает что документ сохранён целиком без разбиения
 * @property uploadedAt Время загрузки и обработки документа
 */
data class DocumentResponse(
    val id: String,
    val filename: String,
    val chunksCount: Int,
    val uploadedAt: LocalDateTime
)
