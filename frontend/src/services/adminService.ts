import axiosInstance from '../utils/axiosConfig';
import { CreateGoodsRequest, UpdateGoodsRequest, PagedResponse, PaginationParams, AdminMedia, MediaReconcileItem, PlantCard } from '../types/admin';
import type { Goods } from '../types/goods';
import type { PhotoDraft } from '../components/admin/MediaManager';

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
    },

    // Media товара (вкладка «Фотографии»)
    getGoodsMedia: async (goodsId: string): Promise<AdminMedia[]> => {
        const response = await axiosInstance.get(`/admin/goods/${goodsId}/media`);
        return response.data;
    },

    uploadGoodsPhoto: async (goodsId: string, blob: Blob): Promise<AdminMedia> => {
        const formData = new FormData();
        formData.append('file', blob, 'photo.jpg');
        const response = await axiosInstance.post(`/admin/goods/${goodsId}/media/upload`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    reconcileGoodsMedia: async (goodsId: string, items: MediaReconcileItem[]): Promise<AdminMedia[]> => {
        const response = await axiosInstance.put(`/admin/goods/${goodsId}/media`, { items });
        return response.data;
    },

    // Анализ фото товара ботом photo-analyzer → автозаполнение карточки
    analyzePhotos: async (photos: PhotoDraft[]): Promise<PlantCard> => {
        const formData = new FormData();
        for (const p of photos) {
            if (p.file) formData.append('files', p.file, 'photo.jpg');
        }
        const mediaIds = photos
            .filter(p => !p.file && p.url?.startsWith('/api/media/') && p.mediaId)
            .map(p => p.mediaId!);
        const urls = photos
            .filter(p => !p.file && p.url && !p.url.startsWith('/api/media/'))
            .map(p => p.url!);
        formData.append('mediaIds', JSON.stringify(mediaIds));
        formData.append('urls', JSON.stringify(urls));
        const response = await axiosInstance.post('/admin/goods/analyze-photos', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    // Заполнение карточки по названию ботом photo-analyzer (без картинок) → автозаполнение
    analyzeByName: async (name: string): Promise<PlantCard> => {
        const response = await axiosInstance.post('/admin/goods/analyze-name', { name });
        return response.data;
    }
};
