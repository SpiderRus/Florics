import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

interface HasPurchasedResponse {
    purchased: boolean;
}

export const purchaseService = {
    hasPurchased: async (plantId: string): Promise<boolean> => {
        try {
            const response = await axios.get<HasPurchasedResponse>(
                `${API_BASE_URL}/purchases/has-purchased/${plantId}`
            );
            return response.data.purchased;
        } catch (error) {
            // Если пользователь не авторизован или ошибка - возвращаем false
            return false;
        }
    }
};
