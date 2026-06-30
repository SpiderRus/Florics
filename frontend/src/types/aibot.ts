/**
 * Типы для интеграции с AI чат-ботом
 */

/**
 * Conversation (разговор) с AI ассистентом
 */
export interface Conversation {
    id: string;
    title: string;
    createdAt: string;
    updatedAt: string;
    messageCount: number;
}

/**
 * Роль сообщения в conversation
 * Backend возвращает enum в uppercase (USER, ASSISTANT, SYSTEM)
 */
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'user' | 'assistant' | 'system';

/**
 * Сообщение в conversation
 */
export interface Message {
    id: string;
    conversationId: string;
    role: MessageRole;
    content: string;
    createdAt: string;
    // Пути backend-прокси к картинкам сообщения (только florarium-чат; иначе отсутствует/пусто)
    imageUrls?: string[];
}

/**
 * Ответ от AI на отправленное сообщение
 */
export interface ChatResponse {
    conversationId: string;
    messageId: string;
    response: string;
    timestamp: string;
}

/**
 * Request для создания conversation для товара
 */
export interface CreateConversationRequest {
    goodsId: string;
    goodsName: string;
}

/**
 * Request для отправки сообщения в чат
 */
export interface ChatMessageRequest {
    message: string;
}

/**
 * Событие SSE-потока дизайнера флорариумов (после JSON.parse поля data).
 * Backend отдаёт текст ИЛИ ссылку на картинку (imageUrl — путь backend-прокси).
 */
export interface FlorariumStreamEvent {
    type: 'text' | 'image';
    text?: string;
    imageUrl?: string;
}

/**
 * Сообщение в чате дизайнера флорариумов.
 * images — objectURL'ы (blob), полученные авторизованной загрузкой картинок.
 */
export interface FlorariumMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    images: string[];
    // Устойчивые прокси-URL картинок (/api/aibot/florarium/images/<id>), выровнены по индексу с images.
    // Нужны для оформления заказа: blob-objectURL из images для заказа не годится (временный).
    imageSources: string[];
    // Сколько картинок ещё догружается — показываем столько же спиннеров-плейсхолдеров
    pendingImages: number;
    createdAt: string;
}
