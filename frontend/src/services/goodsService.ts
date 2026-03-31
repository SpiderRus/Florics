import axios from 'axios';
import { Category } from './categoryService';

const API_BASE_URL = '/api';

export type MediaType = 'image' | 'video';

export interface Media {
    type: MediaType;
    url: string;
    order: number;
}

export interface Goods {
    id: string;
    name: string;
    description: string;
    price: number;
    media: Media[];
    categoryId: string;
    category?: Category;
    difficulty: string;
    duration?: number | null;
    videoUrl?: string | null;
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
    },

    getGoodsByType: async (type: 'PLANT' | 'TERRARIUM' | 'COURSE'): Promise<Goods[]> => {
        const response = await axios.get<Goods[]>(`${API_BASE_URL}/goods/type/${type}`);
        return response.data;
    }
};
