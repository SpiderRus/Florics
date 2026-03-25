import React from 'react';
import {useNavigate} from 'react-router-dom';
import {Container, Button, Card} from 'react-bootstrap';

const CustomTerrariumPage: React.FC = () => {
    const navigate = useNavigate();

    return (
        <Container className="catalog-page">
            <div className="catalog-header">
                <Button className="back-button" onClick={() => navigate('/terrariums')}>
                    ← Назад к каталогу
                </Button>
                <h1>Флорариум под заказ 🪴</h1>
            </div>

            <Card className="auth-card text-center" style={{maxWidth: '600px', margin: '2rem auto'}}>
                <Card.Body>
                    <div style={{fontSize: '4rem'}}>🌱✨</div>
                    <h3 style={{color: 'var(--forest-green)', marginTop: '1rem'}}>
                        Функционал в разработке
                    </h3>
                    <p style={{color: 'var(--warm-brown)', fontSize: '1.1rem', marginTop: '1.5rem'}}>
                        Скоро вы сможете создать уникальный флорариум, выбрав растения,
                        размер контейнера и декоративные элементы.
                    </p>
                    <Button
                        variant="success"
                        onClick={() => navigate('/terrariums')}
                        style={{marginTop: '2rem'}}
                    >
                        Вернуться к каталогу флорариумов
                    </Button>
                </Card.Body>
            </Card>
        </Container>
    );
};

export default CustomTerrariumPage;
