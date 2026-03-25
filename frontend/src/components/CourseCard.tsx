import React, {useState, useEffect} from 'react';
import {Card, Badge, Button} from 'react-bootstrap';
import {useNavigate} from 'react-router-dom';
import {Plant} from '../services/plantService';
import {purchaseService} from '../services/purchaseService';
import {useAuth} from '../contexts/AuthContext';
import ImageCarousel from './ImageCarousel';
import AddToCartButton from './AddToCartButton';

interface CourseCardProps {
    plant: Plant;
}

const CourseCard: React.FC<CourseCardProps> = ({plant}) => {
    const [isPurchased, setIsPurchased] = useState(false);
    const {isAuthenticated} = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (isAuthenticated)
            checkPurchaseStatus();
    }, [isAuthenticated, plant.id]);

    const checkPurchaseStatus = async () => {
        try {
            const result = await purchaseService.hasPurchased(plant.id);
            setIsPurchased(result);
        } catch (error) {
            console.error('Error checking purchase status:', error);
        }
    };

    return (
        <Card className="plant-card h-100" style={{position: 'relative'}}>
            {isPurchased && (
                <div className="purchased-badge">
                    <Badge bg="success">Уже куплено ✓</Badge>
                </div>
            )}
            <ImageCarousel images={plant.images} plantName={plant.name} plantId={plant.id}/>
            <Card.Body className="plant-card-body">
                <h3 className="plant-name">{plant.name}</h3>

                <div className="plant-meta">
                    <Badge bg="secondary" className="me-2">
                        🎥 {plant.category}
                    </Badge>
                    {plant.duration && (
                        <Badge bg="info" className="me-2">
                            🕒 {plant.duration} мин
                        </Badge>
                    )}
                    <Badge bg="primary">{plant.difficulty}</Badge>
                </div>

                <p className="plant-description">{plant.description}</p>
                <div className="plant-price">{plant.price.toFixed(0)} ₽</div>

                {isPurchased ? (
                    <Button
                        variant="success"
                        onClick={() => navigate(`/masterclass/${plant.id}`)}
                        style={{width: '100%', borderRadius: '25px'}}
                    >
                        Смотреть курс →
                    </Button>
                ) : (
                    <AddToCartButton plantId={plant.id} plantName={plant.name}/>
                )}
            </Card.Body>
        </Card>
    );
};

export default CourseCard;
