import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export interface Plant {
    id: string;
    name: string;
    description: string;
    price: number;
    images: string[];
    category: string;
    difficulty: string;
}

export const plantService = {
    getAllPlants: async (): Promise<Plant[]> => {
        const response = await axios.get<Plant[]>(`${API_BASE_URL}/plants`);
        return response.data;
    },

    getPlantById: async (id: number): Promise<Plant> => {
        const response = await axios.get<Plant>(`${API_BASE_URL}/plants/${id}`);
        return response.data;
    }
};
