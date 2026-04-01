import axios from 'axios';
import { CartSummary, CartItem, LocalCartItem, AddToCartRequest, UpdateQuantityRequest, MergeCartRequest, CheckoutResponse } from '../types/cart';

const API_BASE_URL = '/api/cart';
const LOCAL_CART_KEY = 'localCart';

export const cartService = {
    // ========== Серверные операции (требуют авторизации) ==========

    getCart: async (): Promise<CartSummary> => {
        const response = await axios.get<CartSummary>(`${API_BASE_URL}`);
        return response.data;
    },

    addToCart: async (goodsId: string, quantity: number): Promise<CartItem> => {
        const response = await axios.post<CartItem>(
            `${API_BASE_URL}/items`,
            { goodsId, quantity } as AddToCartRequest
        );
        return response.data;
    },

    updateQuantity: async (goodsId: string, quantity: number): Promise<CartItem> => {
        const response = await axios.put<CartItem>(
            `${API_BASE_URL}/items/${goodsId}`,
            { quantity } as UpdateQuantityRequest
        );
        return response.data;
    },

    removeItem: async (goodsId: string): Promise<void> => {
        await axios.delete(`${API_BASE_URL}/items/${goodsId}`);
    },

    clearCart: async (): Promise<void> => {
        await axios.delete(`${API_BASE_URL}`);
    },

    mergeCart: async (localItems: LocalCartItem[]): Promise<CartSummary> => {
        const response = await axios.post<CartSummary>(
            `${API_BASE_URL}/merge`,
            { items: localItems } as MergeCartRequest
        );
        return response.data;
    },

    checkout: async (): Promise<CheckoutResponse> => {
        const response = await axios.post<CheckoutResponse>(`${API_BASE_URL}/checkout`);
        return response.data;
    },

    // ========== Локальные операции (localStorage) ==========

    getLocalCart: (): LocalCartItem[] => {
        const cartJson = localStorage.getItem(LOCAL_CART_KEY);
        return cartJson ? JSON.parse(cartJson) : [];
    },

    saveLocalCart: (items: LocalCartItem[]): void => {
        localStorage.setItem(LOCAL_CART_KEY, JSON.stringify(items));
    },

    addToLocalCart: (goodsId: string, quantity: number): void => {
        const cart = cartService.getLocalCart();
        const existingItem = cart.find(item => item.goodsId === goodsId);

        if (existingItem)
            existingItem.quantity += quantity;
        else
            cart.push({ goodsId, quantity });

        cartService.saveLocalCart(cart);
    },

    updateLocalQuantity: (goodsId: string, quantity: number): void => {
        const cart = cartService.getLocalCart();
        const item = cart.find(item => item.goodsId === goodsId);

        if (item) {
            if (quantity <= 0)
                cartService.removeFromLocalCart(goodsId);
            else {
                item.quantity = quantity;
                cartService.saveLocalCart(cart);
            }
        }
    },

    removeFromLocalCart: (goodsId: string): void => {
        const cart = cartService.getLocalCart().filter(item => item.goodsId !== goodsId);
        cartService.saveLocalCart(cart);
    },

    clearLocalCart: (): void => {
        localStorage.removeItem(LOCAL_CART_KEY);
    },

    getLocalCartCount: (): number => {
        return cartService.getLocalCart().reduce((sum, item) => sum + item.quantity, 0);
    }
};
