package com.example.webflux.service.document

/**
 * Базовое исключение для ошибок взаимодействия с Document Service в AI Agent
 *
 * Выбрасывается при:
 * - Ошибках сети (connection refused, timeout)
 * - 4xx/5xx ответах от AI Agent API
 * - Ошибках парсинга ответа
 */
open class DocumentServiceException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Исключение при timeout запроса к Document Service
 *
 * Выбрасывается когда AI Agent не отвечает в течение readTimeout.
 * Клиент может повторить запрос или показать пользователю сообщение о временной недоступности.
 */
class DocumentUploadTimeoutException(
    message: String,
    cause: Throwable? = null
) : DocumentServiceException(message, cause)
