import React, { useState, useEffect } from 'react';
import { Card, Badge } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { Plant } from '../services/plantService';
import { reviewService } from '../services/reviewService';
import ImageCarousel from './ImageCarousel';
import AddToCartButton from './AddToCartButton';

interface PlantCardProps {
    plant: Plant;
}

const PlantCard: React.FC<PlantCardProps> = ({plant}) => {
    const navigate = useNavigate();
    const [rating, setRating] = useState<{ averageRating: number; totalReviews: number } | null>(null);

    useEffect(() => {
        const loadRating = async () => {
            try {
                const ratingData = await reviewService.getPlantRating(plant.id);
                setRating(ratingData);
            } catch (error) {
                console.error('Error loading rating:', error);
            }
        };
        loadRating();
    }, [plant.id]);

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
        navigate(`/catalog/${plant.id}`);
    };

    return (
        <Card className="plant-card h-100 clickable-card" onClick={handleCardClick} style={{ cursor: 'pointer' }}>
            <div onClick={(e) => e.stopPropagation()}>
                <ImageCarousel images={plant.images} plantName={plant.name} plantId={plant.id}/>
            </div>
            <Card.Body className="plant-card-body">
                <h3 className="plant-name">{plant.name}</h3>

                <div className="plant-meta">
                    <Badge bg="secondary" className="me-2">
                        {plant.category}
                    </Badge>
                    <span className={`difficulty-badge ${getDifficultyClass(plant.difficulty)}`}>
                        {plant.difficulty}
                    </span>
                </div>

                {rating && rating.totalReviews > 0 && (
                    <div className="plant-rating mb-2">
                        ⭐ {rating.averageRating.toFixed(1)} ({rating.totalReviews} {rating.totalReviews === 1 ? 'отзыв' : 'отзывов'})
                    </div>
                )}

                <p className="plant-description">{plant.description}</p>

                <div className="plant-card-footer">
                    <div className="plant-price">{plant.price.toFixed(0)} ₽</div>

                    <div onClick={(e) => e.stopPropagation()}>
                        <AddToCartButton plantId={plant.id} plantName={plant.name} />
                    </div>
                </div>
            </Card.Body>
        </Card>
    );
};

export default PlantCard;
