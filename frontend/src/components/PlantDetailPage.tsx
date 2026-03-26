import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Tabs, Tab, Spinner, Alert } from 'react-bootstrap';
import { plantService, Plant } from '../services/plantService';
import { reviewService } from '../services/reviewService';
import { purchaseService } from '../services/purchaseService';
import { Review } from '../types/review';
import LargeImageCarousel from './LargeImageCarousel';
import ReviewList from './ReviewList';
import ReviewForm from './ReviewForm';
import AddToCartButton from './AddToCartButton';
import { useAuth } from '../contexts/AuthContext';

const PlantDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();

    const [plant, setPlant] = useState<Plant | null>(null);
    const [reviews, setReviews] = useState<Review[]>([]);
    const [hasPurchased, setHasPurchased] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!id) return;

        const loadData = async () => {
            try {
                setLoading(true);
                setError(null);

                const plantData = await plantService.getPlantById(Number(id));
                if (!plantData) {
                    setError('Товар не найден');
                    return;
                }
                setPlant(plantData);

                const reviewsData = await reviewService.getReviews(id);
                setReviews(reviewsData);

                if (isAuthenticated) {
                    const purchased = await purchaseService.hasPurchased(id);
                    setHasPurchased(purchased);
                }
            } catch (err: any) {
                setError('Ошибка при загрузке данных');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [id, isAuthenticated]);

    const handleReviewSubmitted = async () => {
        if (!id) return;
        const updatedReviews = await reviewService.getReviews(id);
        setReviews(updatedReviews);
    };

    if (loading) {
        return (
            <Container className="text-center my-5">
                <Spinner animation="border" variant="success" />
                <p>Загрузка...</p>
            </Container>
        );
    }

    if (error || !plant) {
        return (
            <Container className="my-5">
                <Alert variant="danger">
                    {error || 'Товар не найден'}
                </Alert>
                <button className="btn btn-primary" onClick={() => navigate('/catalog')}>
                    Вернуться в каталог
                </button>
            </Container>
        );
    }

    const canLeaveReview = isAuthenticated && hasPurchased;

    return (
        <Container className="plant-detail-page my-4">
            <Row className="mb-4">
                <Col md={6}>
                    <LargeImageCarousel images={plant.images} plantName={plant.name} />
                </Col>
                <Col md={6} className="plant-detail-info">
                    <h1 className="plant-name">{plant.name}</h1>
                    <p className="plant-description">{plant.description}</p>
                    <div className="plant-meta mb-3">
                        <span className="badge bg-secondary me-2">{plant.category}</span>
                        <span className="badge bg-info">{plant.difficulty}</span>
                    </div>
                    <h3 className="plant-price mb-3">{plant.price.toFixed(0)} ₽</h3>
                    <AddToCartButton plantId={plant.id} plantName={plant.name} />
                </Col>
            </Row>

            <Row>
                <Col>
                    <Tabs defaultActiveKey="description" className="plant-tabs">
                        <Tab eventKey="description" title="Описание">
                            <div className="tab-content-box">
                                {plant.detailedDescription ? (
                                    <p style={{ whiteSpace: 'pre-line' }}>{plant.detailedDescription}</p>
                                ) : (
                                    <p>{plant.description}</p>
                                )}
                            </div>
                        </Tab>
                        <Tab eventKey="care" title="Уход">
                            <div className="tab-content-box">
                                {plant.careInstructions ? (
                                    <p style={{ whiteSpace: 'pre-line' }}>{plant.careInstructions}</p>
                                ) : (
                                    <p>Информация по уходу скоро появится.</p>
                                )}
                            </div>
                        </Tab>
                        <Tab eventKey="reviews" title={`Отзывы (${reviews.length})`}>
                            <div className="tab-content-box">
                                <ReviewList reviews={reviews} />
                                {canLeaveReview ? (
                                    <ReviewForm plantId={plant.id} onReviewSubmitted={handleReviewSubmitted} />
                                ) : isAuthenticated ? (
                                    <Alert variant="info" className="mt-3">
                                        Купите товар, чтобы оставить отзыв
                                    </Alert>
                                ) : (
                                    <Alert variant="info" className="mt-3">
                                        <a href="/login">Войдите</a> и купите товар, чтобы оставить отзыв
                                    </Alert>
                                )}
                            </div>
                        </Tab>
                    </Tabs>
                </Col>
            </Row>
        </Container>
    );
};

export default PlantDetailPage;
