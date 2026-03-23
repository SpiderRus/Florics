import React from 'react';
import {Card, Badge} from 'react-bootstrap';
import {Plant} from '../services/plantService';
import ImageCarousel from './ImageCarousel';
import AddToCartButton from './AddToCartButton';

interface PlantCardProps {
    plant: Plant;
}

const PlantCard: React.FC<PlantCardProps> = ({plant}) => {
    // Определяем класс для badge сложности
    const getDifficultyClass = (difficulty: string): string => {
        switch (difficulty) {
            case 'Легко':
                return 'difficulty-easy';
            case 'Средне':
                return 'difficulty-medium';
            case 'Сложно':
                return 'difficulty-hard';
            default:
                return 'difficulty-medium';
        }
    };

    return (
        <Card className="plant-card h-100">
            <ImageCarousel images={plant.images} plantName={plant.name} plantId={plant.id}/>
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

                <p className="plant-description">{plant.description}</p>

                <div className="plant-price">{plant.price.toFixed(0)} ₽</div>

                <AddToCartButton plantId={plant.id} plantName={plant.name} />
            </Card.Body>
        </Card>
    );
};

export default PlantCard;
