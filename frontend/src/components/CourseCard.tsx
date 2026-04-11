import React, {useState, useEffect} from 'react';
import {Card, Badge, Button} from 'react-bootstrap';
import {useNavigate} from 'react-router-dom';
import {Goods} from '../services/goodsService';
import {purchaseService} from '../services/purchaseService';
import {useAuth} from '../contexts/AuthContext';
import MediaCarousel from './MediaCarousel';
import AddToCartButton from './AddToCartButton';

interface CourseCardProps {
    goods: Goods;
}

const CourseCard: React.FC<CourseCardProps> = ({goods}) => {
    const [isPurchased, setIsPurchased] = useState(false);
    const {user, isAuthenticated} = useAuth();
    const navigate = useNavigate();

    // Показывать кнопку "В корзину" для неавторизованных и для авторизованных с ролью BUYER
    const canAddToCart = !isAuthenticated || user?.canPurchase;

    useEffect(() => {
        if (isAuthenticated)
            checkPurchaseStatus();
    }, [isAuthenticated, goods.id]);

    const checkPurchaseStatus = async () => {
        try {
            const result = await purchaseService.hasPurchased(goods.id);
            setIsPurchased(result);
        } catch (error) {
            console.error('Error checking purchase status:', error);
        }
    };

    const handleCardClick = () => {
        navigate(`/catalog/${goods.id}`);
    };

    return (
        <Card className="goods-card h-100 clickable-card" onClick={handleCardClick} style={{position: 'relative', cursor: 'pointer'}}>
            {isPurchased && (
                <div className="purchased-badge">
                    <Badge bg="success">Уже куплено ✓</Badge>
                </div>
            )}
            <div onClick={(e) => e.stopPropagation()}>
                <MediaCarousel media={goods.media} goodsName={goods.name} goodsId={goods.id}/>
            </div>
            <Card.Body className="goods-card-body">
                <h3 className="goods-name">{goods.name}</h3>

                <div className="goods-meta">
                    <Badge bg="secondary" className="me-2">
                        🎥 {goods.category?.name || 'Без категории'}
                    </Badge>
                    {goods.duration && (
                        <Badge bg="info" className="me-2">
                            🕒 {goods.duration} мин
                        </Badge>
                    )}
                    <Badge bg="primary">{goods.difficulty}</Badge>
                </div>

                <p className="goods-description">{goods.description}</p>

                <div className="goods-card-footer">
                    {!isPurchased && (
                        <div className="goods-price">{goods.price.toFixed(0)} ₽</div>
                    )}

                    <div onClick={(e) => e.stopPropagation()}>
                        {isPurchased ? (
                            <Button
                                variant="success"
                                onClick={() => navigate(`/masterclass/${goods.id}`)}
                                style={{width: '100%', borderRadius: '25px', marginTop: isPurchased ? '1rem' : '0'}}
                            >
                                Смотреть курс →
                            </Button>
                        ) : canAddToCart ? (
                            <AddToCartButton
                                goodsId={goods.id}
                                goodsName={goods.name}
                                isMasterClass={true}
                            />
                        ) : (
                            <Button
                                variant="warning"
                                disabled
                                style={{width: '100%', borderRadius: '25px'}}
                                title="У вас нет прав на покупку"
                            >
                                Недоступно 🚫
                            </Button>
                        )}
                    </div>
                </div>
            </Card.Body>
        </Card>
    );
};

export default CourseCard;
