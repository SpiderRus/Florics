import React, {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Container, Row, Col, Spinner, Alert, Button} from 'react-bootstrap';
import {Goods, goodsService} from '../services/goodsService';
import GoodsCard from './GoodsCard';

const GoodsCatalog: React.FC = () => {
    const navigate = useNavigate();
    const [goods, setGoods] = useState<Goods[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const loadGoods = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await goodsService.getGoodsByType('PLANT');
            setGoods(data);
        } catch (err) {
            console.error('Ошибка загрузки растений:', err);
            setError('Не удалось загрузить каталог растений. Проверьте подключение к серверу.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadGoods();
    }, []);

    if (loading) {
        return (
            <Container className="catalog-page d-flex justify-content-center align-items-center">
                <div className="text-center">
                    <Spinner animation="border" variant="success" role="status">
                        <span className="visually-hidden">Загрузка...</span>
                    </Spinner>
                    <p className="mt-3">Загрузка каталога...</p>
                </div>
            </Container>
        );
    }

    if (error) {
        return (
            <Container className="catalog-page">
                <Alert variant="danger" className="text-center">
                    <Alert.Heading>Ошибка</Alert.Heading>
                    <p>{error}</p>
                    <Button variant="outline-danger" onClick={loadGoods}>
                        Попробовать снова
                    </Button>
                </Alert>
                <div className="text-center mt-3">
                    <Button className="back-button" onClick={() => navigate('/')}>
                        ← Назад на главную
                    </Button>
                </div>
            </Container>
        );
    }

    return (
        <Container className="catalog-page">
            <div className="catalog-header">
                <Button className="back-button" onClick={() => navigate('/')}>
                    ← Назад на главную
                </Button>
                <h1>Каталог комнатных растений</h1>
                <p className="text-muted">Выберите растение для своего дома</p>
            </div>

            <Row className="g-4">
                {goods.map((goods) => (
                    <Col key={goods.id} xs={12} md={6} lg={4}>
                        <GoodsCard goods={goods}/>
                    </Col>
                ))}
            </Row>

            {goods.length === 0 && (
                <Alert variant="info" className="text-center mt-4">
                    Каталог пуст. Растения скоро появятся!
                </Alert>
            )}
        </Container>
    );
};

export default GoodsCatalog;
