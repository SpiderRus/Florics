import React, { useRef, useState } from 'react';

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
    const [hover, setHover] = useState(0);
    const starRefs = useRef<(HTMLSpanElement | null)[]>([]);

    // Только для чтения: статичная картинка с доступной подписью для скринридера
    if (readonly) {
        return (
            <span
                className="star-rating readonly"
                role="img"
                aria-label={`Оценка ${rating} из ${maxRating}`}
            >
                {stars.map((star) => (
                    <span
                        key={star}
                        className={`star ${star <= rating ? 'filled' : 'empty'}`}
                        aria-hidden="true"
                    >
                        {star <= rating ? '⭐' : '☆'}
                    </span>
                ))}
            </span>
        );
    }

    const select = (value: number) => onRatingChange?.(value);

    const handleKeyDown = (e: React.KeyboardEvent, star: number) => {
        let next: number | null = null;
        switch (e.key) {
            case 'ArrowRight':
            case 'ArrowUp':
                next = Math.min(maxRating, star + 1);
                break;
            case 'ArrowLeft':
            case 'ArrowDown':
                next = Math.max(1, star - 1);
                break;
            case 'Home':
                next = 1;
                break;
            case 'End':
                next = maxRating;
                break;
            case ' ':
            case 'Enter':
                e.preventDefault();
                select(star);
                return;
            default:
                return;
        }
        e.preventDefault();
        select(next);
        starRefs.current[next - 1]?.focus();
    };

    // Roving tabindex: в таб-порядок попадает выбранная звезда (или первая, если оценки ещё нет)
    const tabbable = rating >= 1 ? rating : 1;
    const active = hover || rating;

    return (
        <div
            className="star-rating interactive"
            role="radiogroup"
            aria-label="Ваша оценка"
        >
            {stars.map((star) => (
                <span
                    key={star}
                    ref={(el) => { starRefs.current[star - 1] = el; }}
                    role="radio"
                    aria-checked={rating === star}
                    aria-label={`Оценка ${star} из ${maxRating}`}
                    tabIndex={star === tabbable ? 0 : -1}
                    className={`star ${star <= active ? 'filled' : 'empty'}`}
                    onClick={() => select(star)}
                    onMouseEnter={() => setHover(star)}
                    onMouseLeave={() => setHover(0)}
                    onKeyDown={(e) => handleKeyDown(e, star)}
                    style={{ cursor: 'pointer' }}
                >
                    {star <= active ? '⭐' : '☆'}
                </span>
            ))}
        </div>
    );
};

export default StarRating;
