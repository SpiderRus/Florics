import React, { useState, useEffect } from 'react';
import { Card, Badge, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { Goods } from '../services/goodsService';
import { reviewService } from '../services/reviewService';
import { purchaseService } from '../services/purchaseService';
import { GoodsRating } from '../types/review';
import { reviewsWord } from '../utils/plural';
import { useAuth } from '../contexts/AuthContext';
import MediaCarousel from './MediaCarousel';
import AddToCartButton from './AddToCartButton';

interface ProductCardProps {
    goods: Goods;
    // Для курсов статус покупки может прийти из каталога одним запросом (устранение N+1).
    isPurchased?: boolean;
    // Для товаров рейтинг может прийти из каталога одним батч-запросом (устранение N+1).
    rating?: GoodsRating | null;
}

const getDifficultyClass = (difficulty: string): string => {
    if (difficulty === 'Легко' || difficulty.includes('Не требует'))
        return 'difficulty-easy';
    if (difficulty === 'Средне' || difficulty.includes('Минимальный'))
        return 'difficulty-medium';
    if (difficulty === 'Сложно')
        return 'difficulty-hard';
    return 'difficulty-medium';
};

const ProductCard: React.FC<ProductCardProps> = ({ goods, isPurchased: isPurchasedProp, rating: ratingProp }) => {
    const navigate = useNavigate();
    const { user, isAuthenticated } = useAuth();
    const isCourse = goods.category?.type === 'COURSE';

    const [fetchedRating, setFetchedRating] = useState<GoodsRating | null>(null);
    const [fetchedPurchased, setFetchedPurchased] = useState(false);
    const rating = ratingProp !== undefined ? ratingProp : fetchedRating;
    const isPurchased = isPurchasedProp ?? fetchedPurchased;

    // Показывать кнопку "В корзину" для неавторизованных и для авторизованных с ролью BUYER
    const canAddToCart = !isAuthenticated || user?.canPurchase;

    // Рейтинг — только для обычных товаров и только если не передан из каталога (батч)
    useEffect(() => {
        if (isCourse || ratingProp !== undefined) return;
        reviewService.getGoodsRating(goods.id)
            .then(setFetchedRating)
            .catch(error => console.error('Error loading rating:', error));
    }, [goods.id, isCourse, ratingProp]);

    // Статус покупки — только для курсов и только если не передан сверху
    useEffect(() => {
        if (!isCourse || isPurchasedProp !== undefined || !isAuthenticated) return;
        purchaseService.hasPurchased(goods.id)
            .then(setFetchedPurchased)
            .catch(error => console.error('Error checking purchase status:', error));
    }, [isCourse, isAuthenticated, goods.id, isPurchasedProp]);

    const handleCardClick = () => navigate(`/catalog/${goods.id}`);

    // Клавиатурная активация — только когда фокус на самой карточке,
    // чтобы Enter/Space на вложенных контролах не открывали страницу товара
    const handleCardKeyDown = (e: React.KeyboardEvent) => {
        if (e.target !== e.currentTarget) return;
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleCardClick();
        }
    };

    return (
        <Card
            className="goods-card h-100 clickable-card"
            onClick={handleCardClick}
            onKeyDown={handleCardKeyDown}
            role="button"
            tabIndex={0}
            aria-label={`Открыть страницу ${isCourse ? 'курса' : 'товара'}: ${goods.name}`}
            style={{ position: 'relative', cursor: 'pointer' }}
        >
            {isCourse && isPurchased && (
                <div className="purchased-badge">
                    <Badge bg="success">Уже куплено ✓</Badge>
                </div>
            )}

            <div onClick={(e) => e.stopPropagation()}>
                <MediaCarousel media={goods.media} goodsName={goods.name} goodsId={goods.id} />
            </div>

            <Card.Body className="goods-card-body">
                <h2 className="goods-name">{goods.name}</h2>

                <div className="goods-meta">
                    {isCourse ? (
                        <>
                            <Badge bg="secondary" className="me-2">
                                🎥 {goods.category?.name || 'Без категории'}
                            </Badge>
                            {goods.duration && (
                                <Badge bg="info" className="me-2">🕒 {goods.duration} мин</Badge>
                            )}
                            <Badge bg="primary">{goods.difficulty}</Badge>
                        </>
                    ) : (
                        <>
                            <Badge bg="secondary" className="me-2">
                                {goods.category?.name || 'Без категории'}
                            </Badge>
                            <span className={`difficulty-badge ${getDifficultyClass(goods.difficulty)}`}>
                                {goods.difficulty}
                            </span>
                        </>
                    )}
                </div>

                {!isCourse && rating && rating.totalReviews > 0 && (
                    <div className="goods-rating mb-2">
                        ⭐ {rating.averageRating.toFixed(1)} ({rating.totalReviews} {reviewsWord(rating.totalReviews)})
                    </div>
                )}

                <p className="goods-description">{goods.description}</p>

                <div className="goods-card-footer">
                    {(!isCourse || !isPurchased) && (
                        <div className="goods-price">{goods.price.toFixed(0)} ₽</div>
                    )}

                    {isCourse ? (
                        <div onClick={(e) => e.stopPropagation()}>
                            {isPurchased ? (
                                <Button
                                    variant="success"
                                    onClick={() => navigate(`/masterclass/${goods.id}`)}
                                    style={{ width: '100%', borderRadius: 'var(--radius-pill)', marginTop: '1rem' }}
                                >
                                    Смотреть курс →
                                </Button>
                            ) : canAddToCart ? (
                                <AddToCartButton goodsId={goods.id} goodsName={goods.name} isMasterClass={true} />
                            ) : (
                                <Button
                                    variant="warning"
                                    disabled
                                    style={{ width: '100%', borderRadius: 'var(--radius-pill)' }}
                                    title="У вас нет прав на покупку"
                                >
                                    Недоступно 🚫
                                </Button>
                            )}
                        </div>
                    ) : (
                        canAddToCart && (
                            <div onClick={(e) => e.stopPropagation()}>
                                <AddToCartButton goodsId={goods.id} goodsName={goods.name} />
                            </div>
                        )
                    )}
                </div>
            </Card.Body>
        </Card>
    );
};

export default ProductCard;
