import axios from 'axios';
import { Purchase } from '../types/purchase';

const API_BASE_URL = '/api';

interface HasPurchasedResponse {
    purchased: boolean;
}

export const purchaseService = {
    getUserPurchases: async (): Promise<Purchase[]> => {
        const response = await axios.get<Purchase[]>(`${API_BASE_URL}/purchases`);
        return response.data;
    },

    hasPurchased: async (goodsId: string): Promise<boolean> => {
        try {
            const response = await axios.get<HasPurchasedResponse>(
                `${API_BASE_URL}/purchases/has-purchased/${goodsId}`
            );
            return response.data.purchased;
        } catch (error) {
            // Если пользователь не авторизован или ошибка - возвращаем false
            return false;
        }
    }
};
