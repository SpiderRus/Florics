import React, {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Container, Row, Col, Spinner, Alert, Button} from 'react-bootstrap';
import {Plant, plantService} from '../services/plantService';
import PlantCard from './PlantCard';

const PlantCatalog: React.FC = () => {
    const navigate = useNavigate();
    const [plants, setPlants] = useState<Plant[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const loadPlants = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await plantService.getAllPlants();
            setPlants(data);
        } catch (err) {
            console.error('Ошибка загрузки растений:', err);
            setError('Не удалось загрузить каталог растений. Проверьте подключение к серверу.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPlants();
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
                    <Button variant="outline-danger" onClick={loadPlants}>
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
                {plants.map((plant) => (
                    <Col key={plant.id} xs={12} md={6} lg={4}>
                        <PlantCard plant={plant}/>
                    </Col>
                ))}
            </Row>

            {plants.length === 0 && (
                <Alert variant="info" className="text-center mt-4">
                    Каталог пуст. Растения скоро появятся!
                </Alert>
            )}
        </Container>
    );
};

export default PlantCatalog;
