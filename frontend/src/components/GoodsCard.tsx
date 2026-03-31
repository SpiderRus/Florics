import React, { useState, useEffect } from 'react';
import { Card, Badge } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { Goods } from '../services/goodsService';
import { reviewService } from '../services/reviewService';
import MediaCarousel from './MediaCarousel';
import AddToCartButton from './AddToCartButton';

interface GoodsCardProps {
    goods: Goods;
}

const GoodsCard: React.FC<GoodsCardProps> = ({goods}) => {
    const navigate = useNavigate();
    const [rating, setRating] = useState<{ averageRating: number; totalReviews: number } | null>(null);

    useEffect(() => {
        const loadRating = async () => {
            try {
                const ratingData = await reviewService.getGoodsRating(goods.id);
                setRating(ratingData);
            } catch (error) {
                console.error('Error loading rating:', error);
            }
        };
        loadRating();
    }, [goods.id]);

    const getDifficultyClass = (difficulty: string): string => {
        if (difficulty === 'Легко' || difficulty.includes('Не требует'))
            return 'difficulty-easy';
        if (difficulty === 'Средне' || difficulty.includes('Минимальный'))
            return 'difficulty-medium';
        if (difficulty === 'Сложно')
            return 'difficulty-hard';
        return 'difficulty-medium';
    };

    const handleCardClick = () => {
        navigate(`/catalog/${goods.id}`);
    };

    return (
        <Card className="goods-card h-100 clickable-card" onClick={handleCardClick} style={{ cursor: 'pointer' }}>
            <div onClick={(e) => e.stopPropagation()}>
                <MediaCarousel
                    images={goods.images}
                    videoUrls={goods.videoGalleryUrls}
                    goodsName={goods.name}
                    goodsId={goods.id}
                />
            </div>
            <Card.Body className="goods-card-body">
                <h3 className="goods-name">{goods.name}</h3>

                <div className="goods-meta">
                    <Badge bg="secondary" className="me-2">
                        {goods.category?.name || 'Без категории'}
                    </Badge>
                    <span className={`difficulty-badge ${getDifficultyClass(goods.difficulty)}`}>
                        {goods.difficulty}
                    </span>
                </div>

                {rating && rating.totalReviews > 0 && (
                    <div className="goods-rating mb-2">
                        ⭐ {rating.averageRating.toFixed(1)} ({rating.totalReviews} {rating.totalReviews === 1 ? 'отзыв' : 'отзывов'})
                    </div>
                )}

                <p className="goods-description">{goods.description}</p>

                <div className="goods-card-footer">
                    <div className="goods-price">{goods.price.toFixed(0)} ₽</div>

                    <div onClick={(e) => e.stopPropagation()}>
                        <AddToCartButton goodsId={goods.id} goodsName={goods.name} />
                    </div>
                </div>
            </Card.Body>
        </Card>
    );
};

export default GoodsCard;
