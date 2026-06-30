import axiosInstance from '../utils/axiosConfig';
import { CustomOrder, UpdateCustomOrderRequest, ExpertChatSession } from '../types/customOrder';

const API_BASE_URL = '/admin/custom-orders';

const handleStreamUnauthorized = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    const currentPath = window.location.pathname;
    if (currentPath !== '/login' && currentPath !== '/register') {
        localStorage.setItem('redirectAfterLogin', currentPath);
    }
    window.location.href = '/login';
};

export const customOrderService = {
    // Список всех кастомных заказов флорариумов (ADMIN)
    getAllOrders: async (): Promise<CustomOrder[]> => {
        const response = await axiosInstance.get<CustomOrder[]>(API_BASE_URL);
        return response.data;
    },

    // Проставить цену/статус заказа (ADMIN)
    updateOrder: async (id: string, payload: UpdateCustomOrderRequest): Promise<CustomOrder> => {
        const response = await axiosInstance.put<CustomOrder>(`${API_BASE_URL}/${id}`, payload);
        return response.data;
    },

    // Открыть/восстановить экспертный чат мастера по заказу (история + id сессии)
    openExpertChat: async (orderId: string): Promise<ExpertChatSession> => {
        const response = await axiosInstance.post<ExpertChatSession>(`${API_BASE_URL}/${orderId}/expert/session`);
        return response.data;
    },

    /**
     * Отправить сообщение эксперту и получать ответ ПОТОКОМ (SSE).
     * Каждое событие data — JSON-токен ({"t":"..."}); по образцу aiBotService.sendMessageStream.
     */
    streamExpertMessage: async (
        orderId: string,
        message: string,
        onToken: (token: string) => void
    ): Promise<void> => {
        const token = localStorage.getItem('token');

        const response = await fetch(`/api${API_BASE_URL}/${orderId}/expert/chat/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: JSON.stringify({ message })
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

        const flushEvents = () => {
            let sepIndex: number;
            while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
                const rawEvent = buffer.slice(0, sepIndex);
                buffer = buffer.slice(sepIndex + 2);

                const payload = rawEvent
                    .split('\n')
                    .filter(line => line.startsWith('data:'))
                    .map(line => line.slice(5))
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
            buffer += decoder.decode(value, { stream: true }).replace(/\r/g, '');
            flushEvents();
        }

        buffer += decoder.decode();
        if (buffer.trim().length > 0) {
            buffer += '\n\n';
            flushEvents();
        }
    }
};
