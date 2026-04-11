import axiosInstance from '../utils/axiosConfig';
import { CreateGoodsRequest, UpdateGoodsRequest, PagedResponse, PaginationParams } from '../types/admin';
import type { Goods } from '../types/goods';

export const adminService = {
    // Goods CRUD
    getAllGoods: async (params: PaginationParams): Promise<PagedResponse<Goods>> => {
        const response = await axiosInstance.get('/admin/goods', {
            params: {
                page: params.page,
                size: params.size,
                sortBy: params.sortBy,
                sortOrder: params.sortOrder
            }
        });
        return response.data;
    },

    createGoods: async (data: CreateGoodsRequest): Promise<Goods> => {
        const response = await axiosInstance.post('/admin/goods', data);
        return response.data;
    },

    updateGoods: async (id: string, data: UpdateGoodsRequest): Promise<Goods> => {
        const response = await axiosInstance.put(`/admin/goods/${id}`, data);
        return response.data;
    },

    deleteGoods: async (id: string): Promise<void> => {
        await axiosInstance.delete(`/admin/goods/${id}`);
    },

    // File upload
    uploadFile: async (file: File): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await axiosInstance.post('/admin/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data.url;
    }
};
