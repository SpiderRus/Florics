import axios from 'axios';

const API_BASE_URL = '/api';

export interface Goods {
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
    videoGalleryUrls?: string[];
    previewUrl?: string | null;
    detailedDescription?: string | null;
    careInstructions?: string | null;
}

export const goodsService = {
    getAllGoods: async (): Promise<Goods[]> => {
        const response = await axios.get<Goods[]>(`${API_BASE_URL}/goods`);
        return response.data;
    },

    getGoodsById: async (id: number): Promise<Goods> => {
        const response = await axios.get<Goods>(`${API_BASE_URL}/goods/${id}`);
        return response.data;
    }
};
