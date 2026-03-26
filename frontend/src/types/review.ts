export interface Review {
    id: string;
    plantId: string;
    userName: string;
    rating: number;
    comment: string;
    createdAt: string;
    updatedAt: string;
}

export interface CreateReviewRequest {
    plantId: string;
    rating: number;
    comment: string;
}

export interface PlantRating {
    averageRating: number;
    totalReviews: number;
}
