import axios from 'axios';

const API_BASE_URL = '/api';

interface HasPurchasedResponse {
    purchased: boolean;
}

export const purchaseService = {
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
