import React, {useState, useEffect} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {Container, Row, Col, Card, Badge, Button, Spinner} from 'react-bootstrap';
import {Goods, goodsService} from '../services/goodsService';
import {purchaseService} from '../services/purchaseService';
import {useAuth} from '../contexts/AuthContext';

const MasterClassPlayer: React.FC = () => {
    const {id} = useParams<{ id: string }>();
    const {isAuthenticated, loading: authLoading} = useAuth();
    const navigate = useNavigate();
    const [course, setCourse] = useState<Goods | null>(null);
    const [isPurchased, setIsPurchased] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!authLoading) {
            if (!isAuthenticated)
                navigate('/login', {state: {from: `/masterclass/${id}`}});
            else
                loadCourse();
        }
    }, [authLoading, isAuthenticated, id]);

    const loadCourse = async () => {
        try {
            const goods = await goodsService.getGoodsById(Number(id!));
            setCourse(goods);

            const purchased = await purchaseService.hasPurchased(id!);
            setIsPurchased(purchased);
        } catch (error) {
            console.error('Error loading course:', error);
        } finally {
            setLoading(false);
        }
    };

    if (authLoading || loading) {
        return (
            <Container className="d-flex justify-content-center align-items-center" style={{minHeight: '60vh'}}>
                <Spinner animation="border" variant="success"/>
            </Container>
        );
    }

    if (!course) {
        return (
            <Container className="mt-4">
                <Card className="text-center">
                    <Card.Body>
                        <h3>Курс не найден</h3>
                        <Button variant="primary" onClick={() => navigate('/masterclasses')}>
                            Вернуться к каталогу
                        </Button>
                    </Card.Body>
                </Card>
            </Container>
        );
    }

    if (!isPurchased) {
        return (
            <Container className="mt-4">
                <Card className="text-center auth-card" style={{maxWidth: '600px', margin: '2rem auto'}}>
                    <Card.Body>
                        <div style={{fontSize: '4rem'}}>🔒</div>
                        <h3 style={{color: 'var(--forest-green)', marginTop: '1rem'}}>
                            Доступ ограничен
                        </h3>
                        <p style={{color: 'var(--warm-brown)', fontSize: '1.1rem', marginTop: '1.5rem'}}>
                            Сначала купите этот курс, чтобы получить доступ к видео.
                        </p>
                        <Button
                            variant="success"
                            onClick={() => navigate('/masterclasses')}
                            style={{marginTop: '2rem', borderRadius: '25px'}}
                        >
                            Перейти к каталогу курсов
                        </Button>
                    </Card.Body>
                </Card>
            </Container>
        );
    }

    return (
        <Container className="catalog-page" style={{paddingTop: '2rem'}}>
            <Button className="back-button" onClick={() => navigate('/masterclasses')}>
                ← Назад к каталогу
            </Button>

            <Row className="mt-4">
                <Col md={8}>
                    <div
                        className="video-player-stub"
                        style={{
                            width: '100%',
                            aspectRatio: '16/9',
                            backgroundColor: 'var(--light-green)',
                            borderRadius: '15px',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            border: '2px dashed var(--sage-green)'
                        }}
                    >
                        <div style={{textAlign: 'center'}}>
                            <div style={{fontSize: '4rem'}}>🎬</div>
                            <h3 style={{color: 'var(--forest-green)'}}>Видео плеер</h3>
                            <p className="text-muted">Интеграция с Kinescope в разработке</p>
                            <code style={{fontSize: '0.9rem', color: 'var(--sage-green)'}}>
                                Video ID: {course.videoUrl}
                            </code>
                        </div>
                    </div>
                </Col>
                <Col md={4}>
                    <Card>
                        <Card.Body>
                            <h4>{course.name}</h4>
                            <div className="mb-3">
                                <Badge bg="info" className="me-2">
                                    🕒 {course.duration} мин
                                </Badge>
                                <Badge bg="secondary">{course.difficulty}</Badge>
                            </div>
                            <p>{course.description}</p>
                            <div className="plant-price">{course.price.toFixed(0)} ₽</div>
                            <hr/>
                            <Button variant="outline-secondary" disabled style={{width: '100%', borderRadius: '25px'}}>
                                📥 Скачать материалы (скоро)
                            </Button>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
};

export default MasterClassPlayer;
