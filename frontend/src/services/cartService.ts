import axios from 'axios';
import { CartSummary, CartItem, LocalCartItem, AddToCartRequest, UpdateQuantityRequest, MergeCartRequest } from '../types/cart';

const API_BASE_URL = 'http://localhost:8080/api';
const LOCAL_CART_KEY = 'localCart';

export const cartService = {
    // ========== Серверные операции (требуют авторизации) ==========

    getCart: async (): Promise<CartSummary> => {
        const response = await axios.get<CartSummary>(`${API_BASE_URL}/cart`);
        return response.data;
    },

    addToCart: async (plantId: string, quantity: number): Promise<CartItem> => {
        const response = await axios.post<CartItem>(
            `${API_BASE_URL}/cart/items`,
            { plantId, quantity } as AddToCartRequest
        );
        return response.data;
    },

    updateQuantity: async (plantId: string, quantity: number): Promise<CartItem> => {
        const response = await axios.put<CartItem>(
            `${API_BASE_URL}/cart/items/${plantId}`,
            { quantity } as UpdateQuantityRequest
        );
        return response.data;
    },

    removeItem: async (plantId: string): Promise<void> => {
        await axios.delete(`${API_BASE_URL}/cart/items/${plantId}`);
    },

    clearCart: async (): Promise<void> => {
        await axios.delete(`${API_BASE_URL}/cart`);
    },

    mergeCart: async (localItems: LocalCartItem[]): Promise<CartSummary> => {
        const response = await axios.post<CartSummary>(
            `${API_BASE_URL}/cart/merge`,
            { items: localItems } as MergeCartRequest
        );
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

    addToLocalCart: (plantId: string, quantity: number): void => {
        const cart = cartService.getLocalCart();
        const existingItem = cart.find(item => item.plantId === plantId);

        if (existingItem)
            existingItem.quantity += quantity;
        else
            cart.push({ plantId, quantity });

        cartService.saveLocalCart(cart);
    },

    updateLocalQuantity: (plantId: string, quantity: number): void => {
        const cart = cartService.getLocalCart();
        const item = cart.find(item => item.plantId === plantId);

        if (item) {
            if (quantity <= 0)
                cartService.removeFromLocalCart(plantId);
            else {
                item.quantity = quantity;
                cartService.saveLocalCart(cart);
            }
        }
    },

    removeFromLocalCart: (plantId: string): void => {
        const cart = cartService.getLocalCart().filter(item => item.plantId !== plantId);
        cartService.saveLocalCart(cart);
    },

    clearLocalCart: (): void => {
        localStorage.removeItem(LOCAL_CART_KEY);
    },

    getLocalCartCount: (): number => {
        return cartService.getLocalCart().reduce((sum, item) => sum + item.quantity, 0);
    }
};
