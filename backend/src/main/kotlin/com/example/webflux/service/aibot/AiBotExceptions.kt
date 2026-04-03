package com.example.webflux.service.aibot

/**
 * Базовое исключение для AI Bot операций
 */
open class AiBotException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Общая ошибка сервиса AI Agent
 *
 * Используется для ошибок связи, таймаутов, серверных ошибок AI Agent
 */
class AiBotServiceException(message: String, cause: Throwable? = null) : AiBotException(message, cause)

/**
 * Разговор не найден
 *
 * Возникает когда запрашиваемый conversation не существует в AI Agent
 */
class ConversationNotFoundException(conversationId: String) :
    AiBotException("Conversation not found: $conversationId")

/**
 * Доступ к разговору запрещен
 *
 * Возникает когда пользователь пытается получить доступ к conversation другого пользователя
 */
class ConversationAccessDeniedException(conversationId: String) :
    AiBotException("Access denied to conversation: $conversationId")

/**
 * Таймаут запроса к AI Agent
 *
 * Возникает когда AI Agent не отвечает в течение заданного времени
 */
class AiBotTimeoutException(message: String, cause: Throwable? = null) :
    AiBotException(message, cause)
