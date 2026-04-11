import axiosInstance from '../utils/axiosConfig';
import type { Goods, Media, MediaType } from '../types/goods';

// Re-export types for backward compatibility
export type { Goods, Media, MediaType };

export const goodsService = {
    getAllGoods: async (): Promise<Goods[]> => {
        const response = await axiosInstance.get<Goods[]>('/goods');
        return response.data;
    },

    getGoodsById: async (id: string): Promise<Goods> => {
        const response = await axiosInstance.get<Goods>(`/goods/${id}`);
        return response.data;
    },

    getGoodsByType: async (type: 'PLANT' | 'TERRARIUM' | 'COURSE'): Promise<Goods[]> => {
        const response = await axiosInstance.get<Goods[]>(`/goods/type/${type}`);
        return response.data;
    }
};
