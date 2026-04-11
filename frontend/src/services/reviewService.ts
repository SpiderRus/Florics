import axiosInstance from '../utils/axiosConfig';
import { Review, CreateReviewRequest, GoodsRating } from '../types/review';

export const reviewService = {
    getReviews: async (goodsId: string): Promise<Review[]> => {
        const response = await axiosInstance.get<Review[]>(`/reviews/${goodsId}`);
        return response.data;
    },

    getGoodsRating: async (goodsId: string): Promise<GoodsRating> => {
        const response = await axiosInstance.get<GoodsRating>(`/reviews/rating/${goodsId}`);
        return response.data;
    },

    createReview: async (request: CreateReviewRequest): Promise<Review> => {
        const response = await axiosInstance.post<Review>('/reviews', request);
        return response.data;
    }
};
