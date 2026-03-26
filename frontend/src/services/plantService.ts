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
    type?: 'COURSE' | null;
    duration?: number | null;
    videoUrl?: string | null;
    previewUrl?: string | null;
    detailedDescription?: string | null;
    careInstructions?: string | null;
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
