import axiosInstance from '../utils/axiosConfig';
import { Review, CreateReviewRequest, GoodsRating, GoodsRatingItem } from '../types/review';

export const reviewService = {
    getReviews: async (goodsId: string): Promise<Review[]> => {
        const response = await axiosInstance.get<Review[]>(`/reviews/${goodsId}`);
        return response.data;
    },

    getGoodsRating: async (goodsId: string): Promise<GoodsRating> => {
        const response = await axiosInstance.get<GoodsRating>(`/reviews/rating/${goodsId}`);
        return response.data;
    },

    // Рейтинги для списка товаров одним запросом (устранение N+1 в каталоге)
    getRatings: async (ids: string[]): Promise<GoodsRatingItem[]> => {
        if (ids.length === 0) return [];
        const response = await axiosInstance.get<GoodsRatingItem[]>('/reviews/ratings', {
            params: { ids: ids.join(',') }
        });
        return response.data;
    },

    createReview: async (request: CreateReviewRequest): Promise<Review> => {
        const response = await axiosInstance.post<Review>('/reviews', request);
        return response.data;
    }
};
