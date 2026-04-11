import { Category } from '../services/categoryService';

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
    detailedDescription?: string | null;
    careInstructions?: string | null;
}
