import React from 'react';

interface StarRatingProps {
    rating: number;
    maxRating?: number;
    readonly?: boolean;
    onRatingChange?: (rating: number) => void;
}

const StarRating: React.FC<StarRatingProps> = ({ 
    rating, 
    maxRating = 5, 
    readonly = false, 
    onRatingChange 
}) => {
    const stars = Array.from({ length: maxRating }, (_, index) => index + 1);

    const handleClick = (value: number) => {
        if (!readonly && onRatingChange) {
            onRatingChange(value);
        }
    };

    return (
        <div className={`star-rating ${readonly ? 'readonly' : 'interactive'}`}>
            {stars.map((star) => (
                <span
                    key={star}
                    className={`star ${star <= rating ? 'filled' : 'empty'}`}
                    onClick={() => handleClick(star)}
                    style={{ cursor: readonly ? 'default' : 'pointer' }}
                >
                    {star <= rating ? '⭐' : '☆'}
                </span>
            ))}
        </div>
    );
};

export default StarRating;
