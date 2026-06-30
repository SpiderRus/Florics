import axiosInstance from '../utils/axiosConfig';

export interface Category {
    id: string;
    name: string;
    type: 'PLANT' | 'TERRARIUM' | 'COURSE';
}

export const categoryService = {
    getAllCategories: async (): Promise<Category[]> => {
        const response = await axiosInstance.get<Category[]>('/categories');
        return response.data;
    },

    getCategoryById: async (id: string): Promise<Category> => {
        const response = await axiosInstance.get<Category>(`/categories/${id}`);
        return response.data;
    }
};
