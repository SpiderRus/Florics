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
