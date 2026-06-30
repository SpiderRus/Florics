import axiosInstance from '../utils/axiosConfig';
import {
    Conversation,
    Message,
    ChatResponse,
    CreateConversationRequest,
    ChatMessageRequest,
    FlorariumStreamEvent
} from '../types/aibot';

const API_BASE_URL = '/aibot';

/**
 * Обработка 401 для запросов через fetch (минуя axios-интерсептор).
 * Повторяет поведение utils/axiosConfig.ts: чистит сессию и редиректит на /login.
 */
const handleStreamUnauthorized = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    const currentPath = window.location.pathname;
    if (currentPath !== '/login' && currentPath !== '/register') {
        localStorage.setItem('redirectAfterLogin', currentPath);
    }
    window.location.href = '/login';
};

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
            goodsId,
            goodsName
        };
        const response = await axiosInstance.post<Conversation>(`${API_BASE_URL}/conversations`, request);
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
            const response = await axiosInstance.get<Conversation>(`${API_BASE_URL}/conversations/by-goods/${goodsId}`);
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
        const response = await axiosInstance.get<Message[]>(`${API_BASE_URL}/conversations/${conversationId}/messages`, {
            params: { limit }
        });
        return response.data;
    },

    /**
     * Отправить сообщение в чат и получить ответ от AI (целиком, нестримовый fallback)
     *
     * @param conversationId UUID conversation
     * @param message Текст сообщения от пользователя
     * @returns Promise<ChatResponse>
     */
    sendMessage: async (conversationId: string, message: string): Promise<ChatResponse> => {
        const request: ChatMessageRequest = { message };
        const response = await axiosInstance.post<ChatResponse>(`${API_BASE_URL}/chat/${conversationId}`, request);
        return response.data;
    },

    /**
     * Отправить сообщение и получать ответ ПОТОКОМ (SSE).
     *
     * axios не умеет читать потоки в браузере, поэтому используем fetch + ReadableStream.
     * Сервер отдаёт SSE, где в каждом событии `data` лежит JSON-токен ({"t":"..."}) —
     * JSON сохраняет ведущие пробелы в токенах LLM (иначе слова склеиваются).
     *
     * @param conversationId UUID conversation
     * @param message Текст сообщения от пользователя
     * @param onToken Колбэк для каждого пришедшего фрагмента ответа
     */
    sendMessageStream: async (
        conversationId: string,
        message: string,
        onToken: (token: string) => void
    ): Promise<void> => {
        const token = localStorage.getItem('token');

        const response = await fetch(`/api${API_BASE_URL}/chat/${conversationId}/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: JSON.stringify({ message } as ChatMessageRequest)
        });

        // 401 — повторяем поведение axios-интерсептора (см. utils/axiosConfig.ts)
        if (response.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            const currentPath = window.location.pathname;
            if (currentPath !== '/login' && currentPath !== '/register') {
                localStorage.setItem('redirectAfterLogin', currentPath);
            }
            window.location.href = '/login';
            throw new Error('Unauthorized');
        }

        if (!response.ok || !response.body) {
            const error = new Error(`Stream request failed: ${response.status}`) as Error & { status?: number };
            error.status = response.status;
            throw error;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        // Разобрать накопленные SSE-события (разделитель — пустая строка)
        const flushEvents = () => {
            let sepIndex: number;
            while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
                const rawEvent = buffer.slice(0, sepIndex);
                buffer = buffer.slice(sepIndex + 2);

                const payload = rawEvent
                    .split('\n')
                    .filter(line => line.startsWith('data:'))
                    .map(line => line.slice(5)) // убрать префикс 'data:'
                    .join('\n');

                if (!payload) continue;
                try {
                    const parsed = JSON.parse(payload);
                    if (parsed && typeof parsed.t === 'string') {
                        onToken(parsed.t);
                    }
                } catch {
                    // служебные/некорректные события игнорируем
                }
            }
        };

        // eslint-disable-next-line no-constant-condition
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            // \r убираем на уровне SSE-фрейминга (в JSON-токенах \r экранирован, raw CR не встречается)
            buffer += decoder.decode(value, { stream: true }).replace(/\r/g, '');
            flushEvents();
        }

        // Финальный флеш на случай события без хвостового разделителя
        buffer += decoder.decode();
        if (buffer.trim().length > 0) {
            buffer += '\n\n';
            flushEvents();
        }
    },

    /**
     * Удалить conversation (очистить историю)
     *
     * @param conversationId UUID conversation для удаления
     * @returns Promise<void>
     */
    deleteConversation: async (conversationId: string): Promise<void> => {
        await axiosInstance.delete(`${API_BASE_URL}/conversations/${conversationId}`);
    },

    // ===== Дизайнер флорариумов (генерация картинок) — страница /custom-terrarium =====

    /**
     * Создать или получить разговор дизайнера флорариумов (общий, без привязки к товару).
     * Повторный вызов возвращает тот же conversation — бот помнит контекст.
     *
     * @returns Promise<Conversation>
     */
    createOrGetFlorariumConversation: async (): Promise<Conversation> => {
        const response = await axiosInstance.post<Conversation>(`${API_BASE_URL}/florarium/conversations`);
        return response.data;
    },

    /**
     * Начать новый разговор дизайнера флорариумов («Закончить разговор»).
     * Создаёт новый conversation; старый остаётся в истории.
     *
     * @returns Promise<Conversation>
     */
    createNewFlorariumConversation: async (): Promise<Conversation> => {
        const response = await axiosInstance.post<Conversation>(`${API_BASE_URL}/florarium/conversations/new`);
        return response.data;
    },

    /**
     * Загрузить сгенерированную картинку флорариума авторизованным запросом и вернуть objectURL (blob).
     *
     * <img src> не несёт Authorization, а эндпоинт под ролью BUYER — поэтому грузим через fetch
     * с Bearer и создаём blob-URL. Вызывающий обязан освободить его через URL.revokeObjectURL.
     *
     * @param imageUrl Путь backend-прокси вида /api/aibot/florarium/images/<id>
     * @returns Promise<string> objectURL картинки
     */
    fetchFlorariumImage: async (imageUrl: string): Promise<string> => {
        const token = localStorage.getItem('token');
        const response = await fetch(imageUrl, {
            headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) }
        });

        if (response.status === 401) {
            handleStreamUnauthorized();
            throw new Error('Unauthorized');
        }
        if (!response.ok) {
            throw new Error(`Image fetch failed: ${response.status}`);
        }

        const blob = await response.blob();
        return URL.createObjectURL(blob);
    },

    /**
     * Отправить сообщение дизайнеру флорариумов и получать ответ ПОТОКОМ (SSE): текст и/или картинки.
     *
     * Структурно аналогичен sendMessageStream (fetch + ReadableStream), но каждое SSE-событие —
     * это JSON FlorariumStreamEvent ({type:'text',text} либо {type:'image',imageUrl}).
     *
     * @param conversationId UUID conversation
     * @param message Текст сообщения от пользователя
     * @param onEvent Колбэк для каждого пришедшего события (текст или картинка)
     */
    sendFlorariumMessageStream: async (
        conversationId: string,
        message: string,
        onEvent: (event: FlorariumStreamEvent) => void
    ): Promise<void> => {
        const token = localStorage.getItem('token');

        const response = await fetch(`/api${API_BASE_URL}/florarium/chat/${conversationId}/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: JSON.stringify({ message } as ChatMessageRequest)
        });

        if (response.status === 401) {
            handleStreamUnauthorized();
            throw new Error('Unauthorized');
        }

        if (!response.ok || !response.body) {
            const error = new Error(`Stream request failed: ${response.status}`) as Error & { status?: number };
            error.status = response.status;
            throw error;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        // Разобрать накопленные SSE-события (разделитель — пустая строка)
        const flushEvents = () => {
            let sepIndex: number;
            while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
                const rawEvent = buffer.slice(0, sepIndex);
                buffer = buffer.slice(sepIndex + 2);

                const payload = rawEvent
                    .split('\n')
                    .filter(line => line.startsWith('data:'))
                    .map(line => line.slice(5)) // убрать префикс 'data:'
                    .join('\n');

                if (!payload) continue;
                try {
                    const parsed = JSON.parse(payload) as FlorariumStreamEvent;
                    if (parsed && (parsed.type === 'text' || parsed.type === 'image')) {
                        onEvent(parsed);
                    }
                } catch {
                    // служебные/некорректные события игнорируем
                }
            }
        };

        // eslint-disable-next-line no-constant-condition
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true }).replace(/\r/g, '');
            flushEvents();
        }

        // Финальный флеш на случай события без хвостового разделителя
        buffer += decoder.decode();
        if (buffer.trim().length > 0) {
            buffer += '\n\n';
            flushEvents();
        }
    }
};
