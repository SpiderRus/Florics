package com.example.webflux.entity

/**
 * Тип разговора с AI (колонка ai_conversations.type).
 *
 * - [GOODS] — консультация по товару (goods_id заполнен). Значение по умолчанию.
 * - [FLORARIUM] — дизайнер флорариумов с генерацией картинок (goods_id null).
 *
 * В БД и в [AiConversationEntity] хранится как строка (имя enum), как и остальные enum-колонки проекта.
 */
enum class ConversationType {
    GOODS,
    FLORARIUM
}
