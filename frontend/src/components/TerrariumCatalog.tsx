import React, {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Container, Row, Col, Spinner, Alert, Button} from 'react-bootstrap';
import {Plant, plantService} from '../services/plantService';
import PlantCard from './PlantCard';

const TerrariumCatalog: React.FC = () => {
    const navigate = useNavigate();
    const [terrariums, setTerrariums] = useState<Plant[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const loadTerrariums = async () => {
        try {
            setLoading(true);
            setError(null);
            const allPlants = await plantService.getAllPlants();
            const filtered = allPlants.filter(plant => plant.category === 'Флорариум');
            setTerrariums(filtered);
        } catch (err) {
            console.error('Ошибка загрузки флорариумов:', err);
            setError('Не удалось загрузить каталог флорариумов. Проверьте подключение к серверу.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadTerrariums();
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
                    <Button variant="outline-danger" onClick={loadTerrariums}>
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
                <h1>Каталог флорариумов</h1>
                <p className="text-muted">Выберите готовый флорариум или закажите уникальную композицию</p>
            </div>

            <div className="text-center mb-4">
                <Button
                    style={{
                        background: 'var(--leaf-green)',
                        border: 'none',
                        padding: '0.8rem 2rem',
                        borderRadius: '25px',
                        fontWeight: '500'
                    }}
                    onClick={() => navigate('/custom-terrarium')}
                >
                    Создать уникальный флорариум 🪴
                </Button>
            </div>

            <Row className="g-4">
                {terrariums.map((terrarium) => (
                    <Col key={terrarium.id} xs={12} md={6} lg={4}>
                        <PlantCard plant={terrarium}/>
                    </Col>
                ))}
            </Row>

            {terrariums.length === 0 && (
                <Alert variant="info" className="text-center mt-4">
                    Каталог пуст. Флорариумы скоро появятся!
                </Alert>
            )}
        </Container>
    );
};

export default TerrariumCatalog;
