import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Container, Row, Col, Alert, Button, Pagination } from 'react-bootstrap';
import { Goods, goodsService } from '../services/goodsService';
import { purchaseService } from '../services/purchaseService';
import { reviewService } from '../services/reviewService';
import { useAuth } from '../contexts/AuthContext';
import { GoodsRating } from '../types/review';
import { SortOption } from '../hooks/useGoodsFilter';
import ProductCard from './ProductCard';
import LoadingSpinner from './LoadingSpinner';
import CatalogToolbar from './CatalogToolbar';
import Breadcrumbs from './Breadcrumbs';

type CatalogType = 'PLANT' | 'TERRARIUM' | 'COURSE';

const PAGE_SIZE = 9;

// Сопоставление пунктов тулбара с параметрами серверной сортировки
const SORT_MAP: Record<SortOption, { sortBy: string; sortOrder: string }> = {
    'default': { sortBy: 'created_at', sortOrder: 'desc' },
    'name-asc': { sortBy: 'name', sortOrder: 'asc' },
    'price-asc': { sortBy: 'price', sortOrder: 'asc' },
    'price-desc': { sortBy: 'price', sortOrder: 'desc' }
};

interface CatalogConfig {
    title: string;
    crumb: string;
    subtitle: string;
    searchPlaceholder: string;
    emptyText: string;
    errorText: string;
    showCustomFlorariumCta?: boolean;
}

const CONFIG: Record<CatalogType, CatalogConfig> = {
    PLANT: {
        title: 'Каталог комнатных растений',
        crumb: 'Растения',
        subtitle: 'Выберите растение для своего дома',
        searchPlaceholder: 'Поиск растений',
        emptyText: 'Каталог пуст. Растения скоро появятся!',
        errorText: 'Не удалось загрузить каталог растений. Проверьте подключение к серверу.'
    },
    TERRARIUM: {
        title: 'Каталог флорариумов',
        crumb: 'Флорариумы',
        subtitle: 'Выберите готовый флорариум или закажите уникальную композицию',
        searchPlaceholder: 'Поиск флорариумов',
        emptyText: 'Каталог пуст. Флорариумы скоро появятся!',
        errorText: 'Не удалось загрузить каталог флорариумов. Проверьте подключение к серверу.',
        showCustomFlorariumCta: true
    },
    COURSE: {
        title: 'Каталог мастер-классов',
        crumb: 'Мастер-классы',
        subtitle: 'Обучающие видеокурсы по созданию флорариумов и уходу за растениями',
        searchPlaceholder: 'Поиск мастер-классов',
        emptyText: 'Каталог пуст. Мастер-классы скоро появятся!',
        errorText: 'Не удалось загрузить каталог мастер-классов. Проверьте подключение к серверу.'
    }
};

interface CatalogPageProps {
    type: CatalogType;
}

const CatalogPage: React.FC<CatalogPageProps> = ({ type }) => {
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();
    const cfg = CONFIG[type];

    const [query, setQuery] = useState('');               // мгновенное значение поля
    const [debouncedQuery, setDebouncedQuery] = useState(''); // с задержкой → уходит на сервер
    const [sortBy, setSortBy] = useState<SortOption>('default');
    const [page, setPage] = useState(0);
    const [reloadKey, setReloadKey] = useState(0);

    const [goods, setGoods] = useState<Goods[]>([]);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [loaded, setLoaded] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [purchasedIds, setPurchasedIds] = useState<Set<string>>(new Set());
    const [ratings, setRatings] = useState<Map<string, GoodsRating>>(new Map());

    // Сброс состояния при смене типа каталога (компонент переиспользуется между маршрутами)
    useEffect(() => {
        setQuery('');
        setDebouncedQuery('');
        setSortBy('default');
        setPage(0);
        setLoaded(false);
    }, [type]);

    // Дебаунс поиска + возврат на первую страницу
    useEffect(() => {
        const t = setTimeout(() => {
            setDebouncedQuery(query);
            setPage(0);
        }, 350);
        return () => clearTimeout(t);
    }, [query]);

    // Серверная загрузка страницы каталога: поиск + сортировка + пагинация
    useEffect(() => {
        let active = true;
        setLoading(true);
        setError(null);
        const { sortBy: sortField, sortOrder } = SORT_MAP[sortBy];
        goodsService.searchGoods({ type, query: debouncedQuery, sortBy: sortField, sortOrder, page, size: PAGE_SIZE })
            .then(res => {
                if (!active) return;
                setGoods(res.content);
                setTotalElements(res.totalElements);
                setTotalPages(res.totalPages);
            })
            .catch(err => {
                if (active) {
                    console.error('Ошибка загрузки каталога:', err);
                    setError(cfg.errorText);
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false);
                    setLoaded(true);
                }
            });
        return () => { active = false; };
    }, [type, debouncedQuery, sortBy, page, reloadKey]);

    // Покупки (только для курсов) — один запрос истории вместо N в карточках
    useEffect(() => {
        if (type !== 'COURSE' || !isAuthenticated) {
            setPurchasedIds(new Set());
            return;
        }
        let active = true;
        purchaseService.getUserPurchases()
            .then(list => {
                if (active)
                    setPurchasedIds(new Set(list.map(p => p.goodsId).filter((g): g is string => !!g)));
            })
            .catch(() => { /* нет доступа/ошибка — оставляем пустым */ });
        return () => { active = false; };
    }, [type, isAuthenticated]);

    // Рейтинги текущей страницы (не курсы) — один батч-запрос вместо N в карточках
    useEffect(() => {
        if (type === 'COURSE' || goods.length === 0) {
            setRatings(new Map());
            return;
        }
        let active = true;
        reviewService.getRatings(goods.map(g => g.id))
            .then(list => {
                if (active)
                    setRatings(new Map(list.map(r => [r.goodsId, { averageRating: r.averageRating, totalReviews: r.totalReviews }])));
            })
            .catch(() => { /* рейтинги не критичны для отображения каталога */ });
        return () => { active = false; };
    }, [type, goods]);

    // Начальная загрузка
    if (!loaded && loading) {
        return <LoadingSpinner text="Загрузка каталога..." />;
    }

    // Ошибка при начальной загрузке
    if (error && !loaded) {
        return (
            <Container className="catalog-page">
                <Alert variant="danger" className="text-center">
                    <Alert.Heading>Ошибка</Alert.Heading>
                    <p>{error}</p>
                    <Button variant="outline-danger" onClick={() => setReloadKey(k => k + 1)}>
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

    const showToolbar = totalElements > 0 || debouncedQuery.trim().length > 0;

    return (
        <Container className="catalog-page">
            <Breadcrumbs items={[{ label: 'Главная', to: '/' }, { label: cfg.crumb }]} />

            <div className="catalog-header">
                <h1>{cfg.title}</h1>
                <p className="text-muted">{cfg.subtitle}</p>
            </div>

            {cfg.showCustomFlorariumCta && (
                <div className="text-center mb-4">
                    <Button
                        style={{
                            background: 'var(--leaf-green)',
                            border: 'none',
                            padding: '0.8rem 2rem',
                            borderRadius: 'var(--radius-pill)',
                            fontWeight: '500'
                        }}
                        onClick={() => navigate('/custom-terrarium')}
                    >
                        Создать уникальный флорариум 🪴
                    </Button>
                </div>
            )}

            {showToolbar && (
                <CatalogToolbar
                    query={query}
                    onQueryChange={setQuery}
                    sortBy={sortBy}
                    onSortChange={(v) => { setSortBy(v); setPage(0); }}
                    resultCount={totalElements}
                    totalCount={totalElements}
                    searchPlaceholder={cfg.searchPlaceholder}
                />
            )}

            {error && loaded && (
                <Alert variant="danger" className="text-center">{error}</Alert>
            )}

            <Row className="g-4">
                {goods.map((item) => (
                    <Col key={item.id} xs={12} md={6} lg={4}>
                        <ProductCard
                            goods={item}
                            isPurchased={type === 'COURSE' ? purchasedIds.has(item.id) : undefined}
                            rating={type === 'COURSE' ? undefined : (ratings.get(item.id) ?? null)}
                        />
                    </Col>
                ))}
            </Row>

            {!loading && goods.length === 0 && (
                <Alert variant="info" className="text-center mt-4">
                    {debouncedQuery.trim()
                        ? `По запросу «${debouncedQuery}» ничего не найдено.`
                        : cfg.emptyText}
                </Alert>
            )}

            {totalPages > 1 && (
                <div className="d-flex justify-content-center mt-4">
                    <Pagination className="flex-wrap mb-0">
                        <Pagination.Prev
                            disabled={page === 0}
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                        />
                        {Array.from({ length: totalPages }).map((_, i) => (
                            <Pagination.Item key={i} active={i === page} onClick={() => setPage(i)}>
                                {i + 1}
                            </Pagination.Item>
                        ))}
                        <Pagination.Next
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                        />
                    </Pagination>
                </div>
            )}
        </Container>
    );
};

export default CatalogPage;
