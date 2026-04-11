import axiosInstance from '../utils/axiosConfig';
import { Purchase } from '../types/purchase';

interface HasPurchasedResponse {
    purchased: boolean;
}

export const purchaseService = {
    getUserPurchases: async (): Promise<Purchase[]> => {
        const response = await axiosInstance.get<Purchase[]>('/purchases');
        return response.data;
    },

    hasPurchased: async (goodsId: string): Promise<boolean> => {
        try {
            const response = await axiosInstance.get<HasPurchasedResponse>(
                `/purchases/has-purchased/${goodsId}`
            );
            return response.data.purchased;
        } catch (error) {
            // Если пользователь не авторизован или ошибка - возвращаем false
            return false;
        }
    }
};
