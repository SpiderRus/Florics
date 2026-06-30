// Типы кастомных заказов флорариумов (админка) + метки статусов

export type CustomOrderStatus = 'NEW' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export const CUSTOM_ORDER_STATUS_LABELS: Record<CustomOrderStatus, string> = {
    NEW: 'Новый',
    IN_PROGRESS: 'В работе',
    DONE: 'Выполнен',
    CANCELLED: 'Отменён'
};

export const CUSTOM_ORDER_STATUSES: CustomOrderStatus[] = ['NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED'];

export function customOrderStatusLabel(status: string | null | undefined): string {
    if (!status) return '—';
    return (CUSTOM_ORDER_STATUS_LABELS as Record<string, string>)[status] ?? status;
}

export interface CustomOrder {
    id: string;
    userId: string;
    userName: string | null;
    userEmail: string | null;
    conversationId: string | null;
    imageUrl: string | null;
    customerComment: string | null;
    contact: string | null;
    price: number | null;
    status: string | null;
    purchaseDate: string;
}

export interface UpdateCustomOrderRequest {
    price?: number | null;
    status?: string | null;
}

// Сообщение истории экспертного чата (ответ backend, роль в верхнем регистре)
export interface ExpertHistoryMessage {
    id: string;
    role: string; // USER | ASSISTANT | SYSTEM
    content: string;
    createdAt?: string;
}

// Ответ открытия экспертной сессии: id чата, история и картинки дизайна
export interface ExpertChatSession {
    conversationId: string;
    messages: ExpertHistoryMessage[];
    // Все картинки исходного разговора-дизайнера (прокси-URL) — для карусели
    designImages: string[];
    // Выбранная в заказе картинка (активная в карусели)
    selectedImageUrl: string | null;
}

// Сообщение в UI экспертного чата
export interface ExpertMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
}
