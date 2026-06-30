import axiosInstance from '../utils/axiosConfig';
import type { Goods, Media, MediaType } from '../types/goods';

// Re-export types for backward compatibility
export type { Goods, Media, MediaType };

export type GoodsTypeName = 'PLANT' | 'TERRARIUM' | 'COURSE';

export interface PagedGoodsResponse {
    content: Goods[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface SearchGoodsParams {
    type: GoodsTypeName;
    query?: string;
    sortBy: string;     // name | price | created_at
    sortOrder: string;  // asc | desc
    page: number;
    size: number;
}

export const goodsService = {
    getAllGoods: async (): Promise<Goods[]> => {
        const response = await axiosInstance.get<Goods[]>('/goods');
        return response.data;
    },

    getGoodsById: async (id: string): Promise<Goods> => {
        const response = await axiosInstance.get<Goods>(`/goods/${id}`);
        return response.data;
    },

    getGoodsByType: async (type: GoodsTypeName): Promise<Goods[]> => {
        const response = await axiosInstance.get<Goods[]>(`/goods/type/${type}`);
        return response.data;
    },

    // Серверные поиск + сортировка + пагинация для каталога
    searchGoods: async (params: SearchGoodsParams): Promise<PagedGoodsResponse> => {
        const response = await axiosInstance.get<PagedGoodsResponse>('/goods/search', {
            params: {
                type: params.type,
                query: params.query?.trim() || undefined,
                sortBy: params.sortBy,
                sortOrder: params.sortOrder,
                page: params.page,
                size: params.size
            }
        });
        return response.data;
    }
};
