import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Tabs, Tab, Spinner, Alert, Button } from 'react-bootstrap';
import { goodsService, Goods } from '../services/goodsService';
import { reviewService } from '../services/reviewService';
import { purchaseService } from '../services/purchaseService';
import { Review } from '../types/review';
import LargeMediaCarousel, { MediaItem } from './LargeMediaCarousel';
import MediaModal from './MediaModal';
import ReviewList from './ReviewList';
import ReviewForm from './ReviewForm';
import AddToCartButton from './AddToCartButton';
import MarkdownContent from './MarkdownContent';
import { useAuth } from '../contexts/AuthContext';

const GoodsDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { user, isAuthenticated } = useAuth();

    const [goods, setGoods] = useState<Goods | null>(null);
    const [reviews, setReviews] = useState<Review[]>([]);
    const [hasPurchased, setHasPurchased] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [modalMediaItems, setModalMediaItems] = useState<MediaItem[]>([]);
    const [modalCurrentIndex, setModalCurrentIndex] = useState(0);

    useEffect(() => {
        if (!id) return;

        const loadData = async () => {
            try {
                setLoading(true);
                setError(null);

                const goodsData = await goodsService.getGoodsById(Number(id));
                if (!goodsData) {
                    setError('Товар не найден');
                    return;
                }
                setGoods(goodsData);

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

    const handleMediaClick = (mediaItems: MediaItem[], index: number) => {
        setModalMediaItems(mediaItems);
        setModalCurrentIndex(index);
        setShowModal(true);
    };

    if (loading) {
        return (
            <Container className="text-center my-5">
                <Spinner animation="border" variant="success" />
                <p>Загрузка...</p>
            </Container>
        );
    }

    if (error || !goods) {
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
    // Показывать кнопку "В корзину" для неавторизованных и для авторизованных с ролью BUYER
    const canAddToCart = !isAuthenticated || user?.canPurchase;
    // Проверка, является ли товар мастер-классом
    const isMasterClass = goods.category?.type === 'COURSE';
    // Если мастер-класс уже куплен, показывать кнопку просмотра
    const showWatchButton = isMasterClass && hasPurchased && isAuthenticated;
    // Если мастер-класс уже куплен, не показывать кнопку "В корзину"
    const showAddToCartButton = canAddToCart && !showWatchButton;

    return (
        <Container className="goods-detail-page my-4">
            <Row className="mb-4">
                <Col md={6}>
                    <LargeMediaCarousel
                        media={goods.media}
                        goodsName={goods.name}
                        onMediaClick={handleMediaClick}
                    />
                </Col>
                <Col md={6} className="goods-detail-info">
                    <h1 className="goods-name">{goods.name}</h1>
                    <p className="goods-description">{goods.description}</p>
                    <div className="goods-meta mb-3">
                        <span className="badge bg-secondary me-2">{goods.category?.name || 'Без категории'}</span>
                        <span className="badge bg-info">{goods.difficulty}</span>
                        {isMasterClass && goods.duration && (
                            <span className="badge bg-primary ms-2">⏱ {goods.duration} мин</span>
                        )}
                    </div>

                    {!showWatchButton && (
                        <h3 className="goods-price mb-3">{goods.price.toFixed(0)} ₽</h3>
                    )}

                    {showWatchButton && (
                        <Button
                            variant="success"
                            size="lg"
                            onClick={() => navigate(`/masterclass/${goods.id}`)}
                            className="mb-3"
                        >
                            ▶️ Смотреть мастер-класс
                        </Button>
                    )}

                    {showAddToCartButton && (
                        <AddToCartButton goodsId={goods.id} goodsName={goods.name} isMasterClass={isMasterClass} />
                    )}

                    {hasPurchased && !isMasterClass && (
                        <Alert variant="success" className="mt-3">
                            ✓ Вы уже приобрели этот товар
                        </Alert>
                    )}

                    {isAuthenticated && !user?.canPurchase && (
                        <Alert variant="warning" className="mt-3">
                            У вас нет прав на покупку товаров
                        </Alert>
                    )}
                </Col>
            </Row>

            <Row>
                <Col>
                    <Tabs defaultActiveKey="description" className="goods-tabs">
                        <Tab eventKey="description" title="Описание">
                            <div className="tab-content-box">
                                {goods.detailedDescription ? (
                                    <MarkdownContent content={goods.detailedDescription} />
                                ) : (
                                    <p>{goods.description}</p>
                                )}
                            </div>
                        </Tab>
                        <Tab eventKey="care" title="Уход">
                            <div className="tab-content-box">
                                {goods.careInstructions ? (
                                    <MarkdownContent content={goods.careInstructions} />
                                ) : (
                                    <p>Информация по уходу скоро появится.</p>
                                )}
                            </div>
                        </Tab>
                        <Tab eventKey="reviews" title={`Отзывы (${reviews.length})`}>
                            <div className="tab-content-box">
                                <ReviewList reviews={reviews} />
                                {canLeaveReview ? (
                                    <ReviewForm goodsId={goods.id} onReviewSubmitted={handleReviewSubmitted} />
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

            <MediaModal
                show={showModal}
                mediaItems={modalMediaItems}
                currentIndex={modalCurrentIndex}
                onHide={() => setShowModal(false)}
                onNavigate={setModalCurrentIndex}
            />
        </Container>
    );
};

export default GoodsDetailPage;
