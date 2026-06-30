import React, {useState, useEffect} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {Container, Row, Col, Card, Badge, Button} from 'react-bootstrap';
import LoadingSpinner from './LoadingSpinner';
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
            const goods = await goodsService.getGoodsById(id!);
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
        return <LoadingSpinner text="Загрузка курса..." />;
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
                            style={{marginTop: '2rem', borderRadius: 'var(--radius-pill)'}}
                        >
                            Перейти к каталогу курсов
                        </Button>
                    </Card.Body>
                </Card>
            </Container>
        );
    }

    // YouTube/Vimeo watch-ссылки → embed-URL; остальное (kinescope и т.п.) считаем уже embed
    const toEmbedUrl = (url: string): string => {
        const yt = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([\w-]{11})/);
        if (yt) return `https://www.youtube.com/embed/${yt[1]}`;
        const vimeo = url.match(/vimeo\.com\/(\d+)/);
        if (vimeo) return `https://player.vimeo.com/video/${vimeo[1]}`;
        return url;
    };

    const renderVideo = (url: string | null | undefined, title: string) => {
        if (!url)
            return (
                <div className="masterclass-video-empty">
                    <div style={{textAlign: 'center'}}>
                        <div style={{fontSize: '3rem'}} aria-hidden="true">🎬</div>
                        <p className="text-muted mb-0">Видео скоро появится</p>
                    </div>
                </div>
            );
        // Прямой видеофайл — нативный плеер с полными контролами
        if (/\.(mp4|webm|ogg|mov|m4v)(\?.*)?$/i.test(url))
            return (
                <video key={url} className="masterclass-video" src={url} controls controlsList="nodownload" preload="metadata">
                    Ваш браузер не поддерживает воспроизведение видео.
                </video>
            );
        // Иначе — встраиваемый плеер (YouTube/Vimeo/Kinescope)
        return (
            <div className="masterclass-video-frame">
                <iframe
                    src={toEmbedUrl(url)}
                    title={title}
                    allow="autoplay; fullscreen; picture-in-picture; encrypted-media"
                    allowFullScreen
                />
            </div>
        );
    };

    return (
        <Container className="catalog-page" style={{paddingTop: '2rem'}}>
            <Row className="mt-4">
                <Col md={8}>
                    {renderVideo(course.videoUrl, course.name)}
                </Col>
                <Col md={4}>
                    <Card>
                        <Card.Body>
                            <h4>{course.name}</h4>
                            <div className="mb-3">
                                <Badge bg="success" className="me-2">
                                    ✓ Куплено
                                </Badge>
                                <Badge bg="info" className="me-2">
                                    🕒 {course.duration} мин
                                </Badge>
                                <Badge bg="secondary">{course.difficulty}</Badge>
                            </div>
                            <p>{course.description}</p>
                            {course.detailedDescription && (
                                <>
                                    <hr/>
                                    <p style={{fontSize: '0.9rem', whiteSpace: 'pre-line'}}>
                                        {course.detailedDescription}
                                    </p>
                                </>
                            )}
                            <hr/>
                            <Button variant="outline-secondary" disabled style={{width: '100%', borderRadius: 'var(--radius-pill)'}}>
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
