import axios from 'axios';
import {
    Conversation,
    Message,
    ChatResponse,
    CreateConversationRequest,
    ChatMessageRequest
} from '../types/aibot';

const API_BASE_URL = '/api/aibot';

/**
 * Сервис для работы с AI чат-ботом
 *
 * Предоставляет методы для:
 * - Создания/получения conversation для товара
 * - Получения истории сообщений
 * - Отправки сообщений и получения ответов от AI
 * - Удаления conversation (очистка истории)
 */
export const aiBotService = {
    /**
     * Создать или получить существующий conversation для товара
     *
     * Если conversation для данного товара уже существует, вернет его.
     * Иначе создаст новый.
     *
     * @param goodsId ID товара
     * @param goodsName Название товара
     * @returns Promise<Conversation>
     */
    createOrGetConversation: async (goodsId: string, goodsName: string): Promise<Conversation> => {
        const request: CreateConversationRequest = {
            goodId: goodsId,
            goodsName
        };
        const response = await axios.post<Conversation>(`${API_BASE_URL}/conversations`, request);
        return response.data;
    },

    /**
     * Получить conversation для товара
     *
     * @param goodsId ID товара
     * @returns Promise<Conversation | null> - null если conversation не найден
     */
    getConversationByGoods: async (goodsId: string): Promise<Conversation | null> => {
        try {
            const response = await axios.get<Conversation>(`${API_BASE_URL}/conversations/by-goods/${goodsId}`);
            return response.data;
        } catch (error: any) {
            if (error.response?.status === 404) {
                return null;
            }
            throw error;
        }
    },

    /**
     * Получить историю сообщений conversation
     *
     * @param conversationId UUID conversation
     * @param limit Максимальное количество сообщений (по умолчанию 50)
     * @returns Promise<Message[]>
     */
    getMessages: async (conversationId: string, limit: number = 50): Promise<Message[]> => {
        const response = await axios.get<Message[]>(`${API_BASE_URL}/conversations/${conversationId}/messages`, {
            params: { limit }
        });
        return response.data;
    },

    /**
     * Отправить сообщение в чат и получить ответ от AI
     *
     * @param conversationId UUID conversation
     * @param message Текст сообщения от пользователя
     * @returns Promise<ChatResponse>
     */
    sendMessage: async (conversationId: string, message: string): Promise<ChatResponse> => {
        const request: ChatMessageRequest = { message };
        const response = await axios.post<ChatResponse>(`${API_BASE_URL}/chat/${conversationId}`, request);
        return response.data;
    },

    /**
     * Удалить conversation (очистить историю)
     *
     * @param conversationId UUID conversation для удаления
     * @returns Promise<void>
     */
    deleteConversation: async (conversationId: string): Promise<void> => {
        await axios.delete(`${API_BASE_URL}/conversations/${conversationId}`);
    }
};
