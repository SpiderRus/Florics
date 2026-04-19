import { useNavigate } from 'react-router-dom'
import { Container } from 'react-bootstrap'

function HomePage() {
    const navigate = useNavigate()

    return (
        <>
            <div className="hero-section">
                <Container>
                    <h1 className="hero-title">Добро пожаловать в GreenDecor</h1>
                    <p className="hero-subtitle">
                        Ваш магазин красивых растений и флорариумов
                    </p>
                    <div className="hero-description">
                        <p>Откройте для себя нашу коллекцию живых растений, флорариумов ручной работы и ботанических
                            аксессуаров.</p>
                        <p>Приносим красоту природы в ваш дом, по одному растению за раз.</p>
                    </div>
                </Container>
            </div>

            <Container className="content-section">
                <div className="feature-grid">
                    <div
                        className="feature-card clickable-card"
                        onClick={() => navigate('/catalog')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon">🪴</div>
                        <h3>Комнатные растения</h3>
                        <p>Широкий выбор комнатных растений для любого пространства</p>
                    </div>
                    <div
                        className="feature-card clickable-card"
                        onClick={() => navigate('/terrariums')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon">🌱</div>
                        <h3>Флорариумы</h3>
                        <p>Стеклянные флорариумы ручной работы с живыми экосистемами</p>
                    </div>
                    <div
                        className="feature-card clickable-card"
                        onClick={() => navigate('/masterclasses')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon">🎓</div>
                        <h3>Мастер-классы</h3>
                        <p>Обучающие видеокурсы по созданию флорариумов и уходу за растениями</p>
                    </div>
                </div>
            </Container>
        </>
    )
}

export default HomePage
