import React from 'react';
import { Review } from '../types/review';
import StarRating from './StarRating';

interface ReviewListProps {
    reviews: Review[];
}

const ReviewList: React.FC<ReviewListProps> = ({ reviews }) => {
    if (reviews.length === 0) {
        return (
            <div className="no-reviews">
                <p>Пока нет отзывов. Будьте первым, кто оставит отзыв!</p>
            </div>
        );
    }

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    return (
        <div className="review-list">
            {reviews.map((review) => (
                <div key={review.id} className="review-card">
                    <div className="review-header">
                        <div className="review-author">
                            <strong>{review.userName}</strong>
                            <span className="review-date">{formatDate(review.createdAt)}</span>
                        </div>
                        <StarRating rating={review.rating} readonly={true} />
                    </div>
                    <div className="review-comment">{review.comment}</div>
                </div>
            ))}
        </div>
    );
};

export default ReviewList;
