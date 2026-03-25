import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export interface Purchase {
    id: string;
    userId: number;
    plantId: string;
    price: number;
    purchaseDate: string;
    quantity: number;
}

export const purchaseService = {
    getUserPurchases: async (): Promise<Purchase[]> => {
        const response = await axios.get<{ purchases: Purchase[] }>(`${API_BASE_URL}/purchases`);
        return response.data.purchases;
    },

    hasPurchased: async (plantId: string): Promise<boolean> => {
        const response = await axios.get<{ purchased: boolean }>(`${API_BASE_URL}/purchases/has-purchased/${plantId}`);
        return response.data.purchased;
    }
};
