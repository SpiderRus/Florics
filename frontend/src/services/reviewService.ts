import axios from 'axios';
import { Review, CreateReviewRequest, PlantRating } from '../types/review';

const API_BASE_URL = 'http://localhost:8080/api';

export const reviewService = {
    getReviews: async (plantId: string): Promise<Review[]> => {
        const response = await axios.get<Review[]>(`${API_BASE_URL}/reviews/${plantId}`);
        return response.data;
    },

    getPlantRating: async (plantId: string): Promise<PlantRating> => {
        const response = await axios.get<PlantRating>(`${API_BASE_URL}/reviews/rating/${plantId}`);
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
