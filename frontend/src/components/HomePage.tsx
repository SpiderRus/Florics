import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Container, Row, Col } from 'react-bootstrap'
import { Goods, goodsService } from '../services/goodsService'
import { reviewService } from '../services/reviewService'
import { GoodsRating } from '../types/review'
import ProductCard from './ProductCard'

function HomePage() {
    const navigate = useNavigate()
    const [popular, setPopular] = useState<Goods[]>([])
    const [ratings, setRatings] = useState<Map<string, GoodsRating>>(new Map())

    // Несколько товаров для блока «Популярное» + их рейтинги одним батч-запросом.
    // Ошибку глотаем — секция просто не покажется.
    useEffect(() => {
        let active = true
        goodsService.getGoodsByType('PLANT')
            .then(list => {
                if (!active) return
                const top = list.slice(0, 3)
                setPopular(top)
                return reviewService.getRatings(top.map(g => g.id))
            })
            .then(items => {
                if (active && items)
                    setRatings(new Map(items.map(r => [r.goodsId, { averageRating: r.averageRating, totalReviews: r.totalReviews }])))
            })
            .catch(() => { /* секция популярного не отображается */ })
        return () => { active = false }
    }, [])

    const cardKeyDown = (path: string) => (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            navigate(path)
        }
    }

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
                        role="button"
                        tabIndex={0}
                        aria-label="Перейти в каталог комнатных растений"
                        onClick={() => navigate('/catalog')}
                        onKeyDown={cardKeyDown('/catalog')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon" aria-hidden="true">🪴</div>
                        <h2>Комнатные растения</h2>
                        <p>Широкий выбор комнатных растений для любого пространства</p>
                    </div>
                    <div
                        className="feature-card clickable-card"
                        role="button"
                        tabIndex={0}
                        aria-label="Перейти в каталог флорариумов"
                        onClick={() => navigate('/terrariums')}
                        onKeyDown={cardKeyDown('/terrariums')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon" aria-hidden="true">🌱</div>
                        <h2>Флорариумы</h2>
                        <p>Стеклянные флорариумы ручной работы с живыми экосистемами</p>
                    </div>
                    <div
                        className="feature-card clickable-card"
                        role="button"
                        tabIndex={0}
                        aria-label="Перейти к мастер-классам"
                        onClick={() => navigate('/masterclasses')}
                        onKeyDown={cardKeyDown('/masterclasses')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon" aria-hidden="true">🎓</div>
                        <h2>Мастер-классы</h2>
                        <p>Обучающие видеокурсы по созданию флорариумов и уходу за растениями</p>
                    </div>
                </div>
            </Container>

            {popular.length > 0 && (
                <Container className="content-section">
                    <div className="toolbar" style={{marginBottom: '1.5rem'}}>
                        <h2 style={{color: 'var(--forest-green)', margin: 0}}>Популярные растения</h2>
                        <Link to="/catalog" className="home-see-all">Смотреть все →</Link>
                    </div>
                    <Row className="g-4">
                        {popular.map((item) => (
                            <Col key={item.id} xs={12} md={6} lg={4}>
                                <ProductCard goods={item} rating={ratings.get(item.id) ?? null}/>
                            </Col>
                        ))}
                    </Row>
                </Container>
            )}
        </>
    )
}

export default HomePage
