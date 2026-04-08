export interface Review {
    goodsId: string;
    userId: string;
    userName: string;
    rating: number;
    comment: string;
    createdAt: string;
    updatedAt: string;
}

export interface CreateReviewRequest {
    goodsId: string;
    rating: number;
    comment: string;
}

export interface GoodsRating {
    averageRating: number;
    totalReviews: number;
}
