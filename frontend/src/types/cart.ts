import { Goods } from '../services/goodsService';

export interface CartItem {
    id: string;
    goods: Goods;
    quantity: number;
    addedAt: string;
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
    goodsId: string;
    goodsName: string;
    quantity: number;
    price: number;
}
