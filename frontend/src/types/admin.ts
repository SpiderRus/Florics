export interface CreateGoodsRequest {
    name: string;
    description: string;
    price: number;
    categoryId: string;
    difficulty?: string;
    duration?: number;
    videoUrl?: string;
    detailedDescription?: string;
    careInstructions?: string;
}

export interface UpdateGoodsRequest extends CreateGoodsRequest {
    id: string;
}

export interface PagedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

export type SortField = 'name' | 'category' | 'price' | 'created_at';
export type SortOrder = 'asc' | 'desc';

export interface PaginationParams {
    page: number;
    size: number;
    sortBy: SortField;
    sortOrder: SortOrder;
}
