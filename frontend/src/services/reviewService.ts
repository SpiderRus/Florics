import axios from 'axios';
import { Review, CreateReviewRequest, GoodsRating } from '../types/review';

const API_BASE_URL = '/api';

export const reviewService = {
    getReviews: async (goodsId: string): Promise<Review[]> => {
        const response = await axios.get<Review[]>(`${API_BASE_URL}/reviews/${goodsId}`);
        return response.data;
    },

    getGoodsRating: async (goodsId: string): Promise<GoodsRating> => {
        const response = await axios.get<GoodsRating>(`${API_BASE_URL}/reviews/rating/${goodsId}`);
        return response.data;
    },

    createReview: async (request: CreateReviewRequest): Promise<Review> => {
        const response = await axios.post<Review>(`${API_BASE_URL}/reviews`, request);
        return response.data;
    },

    deleteReview: async (reviewId: string): Promise<void> => {
        await axios.delete(`${API_BASE_URL}/reviews/${reviewId}`);
    }
};
