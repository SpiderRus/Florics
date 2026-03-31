import React, {useState, useEffect} from 'react';
import {Card, Badge, Button} from 'react-bootstrap';
import {useNavigate} from 'react-router-dom';
import {Goods} from '../services/goodsService';
import {purchaseService} from '../services/purchaseService';
import {useAuth} from '../contexts/AuthContext';
import ImageCarousel from './ImageCarousel';
import AddToCartButton from './AddToCartButton';

interface CourseCardProps {
    goods: Goods;
}

const CourseCard: React.FC<CourseCardProps> = ({goods}) => {
    const [isPurchased, setIsPurchased] = useState(false);
    const {isAuthenticated} = useAuth();
    const navigate = useNavigate();

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

    return (
        <Card className="goods-card h-100" style={{position: 'relative'}}>
            {isPurchased && (
                <div className="purchased-badge">
                    <Badge bg="success">Уже куплено ✓</Badge>
                </div>
            )}
            <ImageCarousel images={goods.images} goodsName={goods.name} goodsId={goods.id}/>
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
                    <div className="goods-price">{goods.price.toFixed(0)} ₽</div>

                    {isPurchased ? (
                        <Button
                            variant="success"
                            onClick={() => navigate(`/masterclass/${goods.id}`)}
                            style={{width: '100%', borderRadius: '25px'}}
                        >
                            Смотреть курс →
                        </Button>
                    ) : (
                        <AddToCartButton goodsId={goods.id} goodsName={goods.name}/>
                    )}
                </div>
            </Card.Body>
        </Card>
    );
};

export default CourseCard;
