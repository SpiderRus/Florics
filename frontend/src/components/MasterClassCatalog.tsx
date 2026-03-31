import React, {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Container, Row, Col, Spinner, Alert, Button} from 'react-bootstrap';
import {Goods, goodsService} from '../services/goodsService';
import CourseCard from './CourseCard';

const MasterClassCatalog: React.FC = () => {
    const navigate = useNavigate();
    const [courses, setCourses] = useState<Goods[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const loadCourses = async () => {
        try {
            setLoading(true);
            setError(null);
            const allPlants = await goodsService.getAllGoods();
            const filtered = allPlants.filter(goods => goods.category?.type === 'COURSE');
            setCourses(filtered);
        } catch (err) {
            console.error('Ошибка загрузки мастер-классов:', err);
            setError('Не удалось загрузить каталог мастер-классов. Проверьте подключение к серверу.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCourses();
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
                    <Button variant="outline-danger" onClick={loadCourses}>
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
                <h1>Каталог мастер-классов</h1>
                <p className="text-muted">Обучающие видеокурсы по созданию флорариумов и уходу за растениями</p>
            </div>

            <Row className="g-4">
                {courses.map((course) => (
                    <Col key={course.id} xs={12} md={6} lg={4}>
                        <CourseCard goods={course}/>
                    </Col>
                ))}
            </Row>

            {courses.length === 0 && (
                <Alert variant="info" className="text-center mt-4">
                    Каталог пуст. Мастер-классы скоро появятся!
                </Alert>
            )}
        </Container>
    );
};

export default MasterClassCatalog;
