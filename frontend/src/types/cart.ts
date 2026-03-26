import { Plant } from '../services/plantService';

export interface CartItem {
    id: string;
    plant: Plant;
    quantity: number;
    addedAt: string;
}

export interface CartSummary {
    items: CartItem[];
    totalItems: number;
    totalPrice: number;
}

export interface LocalCartItem {
    plantId: string;
    quantity: number;
}

export interface AddToCartRequest {
    plantId: string;
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
    plantId: string;
    plantName: string;
    quantity: number;
    price: number;
}
