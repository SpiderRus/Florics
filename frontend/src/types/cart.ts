import { Goods } from '../services/goodsService';

export type CartItemKind = 'GOODS' | 'CUSTOM_FLORARIUM';

export interface CartItem {
    id: string;
    kind: CartItemKind;
    goods: Goods | null;            // null для кастомного флорариума
    quantity: number;
    addedAt: string;
    // Поля кастомного заказа (заполнены для kind === 'CUSTOM_FLORARIUM')
    conversationId?: string | null;
    imageUrl?: string | null;
    customerComment?: string | null;
    contact?: string | null;
}

export interface AddCustomFlorariumRequest {
    conversationId: string;
    imageUrl: string;
    comment?: string;
    contact?: string;
}

export interface CartSummary {
    items: CartItem[];
    totalItems: number;
    totalPrice: number;
}

export interface LocalCartItem {
    goodsId: string;
    quantity: number;
}

export interface AddToCartRequest {
    goodsId: string;
    quantity: number;
}

export interface UpdateQuantityRequest {
    quantity: number;
}

export interface MergeCartRequest {
    items: LocalCartItem[];
}

export interface CheckoutResponse {
    orderId: string;
    totalPrice: number;
    items: PurchasedItem[];
    purchaseDate: string;
}

export interface PurchasedItem {
    goodsId: string | null;
    goodsName: string;
    quantity: number;
    price: number | null;
}
