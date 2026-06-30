import React from 'react';
import { Container, Card, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';

const NotFound: React.FC = () => {
    const navigate = useNavigate();

    return (
        <Container style={{ paddingTop: '3rem', paddingBottom: '3rem', textAlign: 'center' }}>
            <Card className="auth-card" style={{ margin: '0 auto' }}>
                <Card.Body>
                    <div style={{ fontSize: '4rem' }} aria-hidden="true">🪴</div>
                    <h1 style={{ color: 'var(--forest-green)' }}>404</h1>
                    <h2>Такой страницы нет</h2>
                    <p className="text-muted">
                        Возможно, ссылка устарела или вы ошиблись адресом. Вернитесь в каталог — там точно что-то прорастёт.
                    </p>
                    <div className="d-flex flex-wrap justify-content-center gap-2 mt-3">
                        <Button variant="outline-secondary" onClick={() => navigate(-1)}>
                            Назад
                        </Button>
                        <Button variant="success" onClick={() => navigate('/')}>
                            На главную 🌿
                        </Button>
                    </div>
                </Card.Body>
            </Card>
        </Container>
    );
};

export default NotFound;
