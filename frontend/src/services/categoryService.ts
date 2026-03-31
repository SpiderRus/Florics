import axios from 'axios';

const API_BASE_URL = '/api';

export interface Category {
    id: string;
    name: string;
    type: 'PHYSICAL' | 'COURSE';
}

export const categoryService = {
    getAllCategories: async (): Promise<Category[]> => {
        const response = await axios.get<Category[]>(`${API_BASE_URL}/categories`);
        return response.data;
    },

    getCategoryById: async (id: string): Promise<Category> => {
        const response = await axios.get<Category>(`${API_BASE_URL}/categories/${id}`);
        return response.data;
    }
};
