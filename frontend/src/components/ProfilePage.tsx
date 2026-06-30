import { useEffect, useState, useMemo } from 'react';
import { Container, Row, Col, Card, Table, Alert, Pagination, Form, Button } from 'react-bootstrap';
import LoadingSpinner from './LoadingSpinner';
import { useAuth } from '../contexts/AuthContext';
import { purchaseService } from '../services/purchaseService';
import { goodsService, Goods } from '../services/goodsService';
import { Purchase } from '../types/purchase';
import { customOrderStatusLabel } from '../types/customOrder';
import { useNavigate, Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { purchasesWord } from '../utils/plural';

interface PurchaseWithGoods extends Purchase {
    goods?: Goods;
}

type SortField = 'date' | 'price' | 'name';
type SortOrder = 'asc' | 'desc';

function ProfilePage() {
    const { user, isAuthenticated, loading: authLoading } = useAuth();
    const navigate = useNavigate();
    const isMobile = useMediaQuery('(max-width: 767.98px)');
    const [purchases, setPurchases] = useState<PurchaseWithGoods[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [, setGoodsMap] = useState<Map<string, Goods>>(new Map());

    // Пагинация и фильтрация
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage] = useState(10);
    const [sortField, setSortField] = useState<SortField>('date');
    const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
    const [categoryFilter, setCategoryFilter] = useState<string>('all');

    useEffect(() => {
        // Ждем, пока AuthContext загрузится
        if (authLoading) {
            return;
        }

        if (!isAuthenticated) {
            navigate('/login', { state: { from: '/profile' } });
            return;
        }

        loadPurchases();
    }, [isAuthenticated, authLoading, navigate]);

    const loadPurchases = async () => {
        try {
            setLoading(true);
            setError(null);

            // Загружаем покупки
            const purchasesData = await purchaseService.getUserPurchases();

            // Загружаем все товары
            const allGoods = await goodsService.getAllGoods();
            const goodsMapTemp = new Map<string, Goods>();
            allGoods.forEach(goods => goodsMapTemp.set(goods.id, goods));
            setGoodsMap(goodsMapTemp);

            // Обогащаем покупки информацией о товарах
            const enrichedPurchases = purchasesData.map(purchase => ({
                ...purchase,
                goods: purchase.goodsId ? goodsMapTemp.get(purchase.goodsId) : undefined
            }));

            setPurchases(enrichedPurchases);
        } catch (err) {
            console.error('Error loading purchases:', err);
            setError('Не удалось загрузить историю покупок');
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('ru-RU', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const totalAmount = purchases.reduce((sum, purchase) => sum + (purchase.price ?? 0) * purchase.quantity, 0);

    // Статистика по категориям
    const categoryStats = useMemo(() => {
        const stats = new Map<string, { count: number; total: number; name: string }>();

        purchases.forEach(purchase => {
            if (purchase.goods?.category) {
                const categoryName = purchase.goods.category.name;
                const existing = stats.get(categoryName) || { count: 0, total: 0, name: categoryName };
                existing.count += purchase.quantity;
                existing.total += (purchase.price ?? 0) * purchase.quantity;
                stats.set(categoryName, existing);
            }
        });

        return Array.from(stats.values()).sort((a, b) => b.total - a.total);
    }, [purchases]);

    // График покупок по месяцам
    const monthlyStats = useMemo(() => {
        const stats = new Map<string, number>();

        purchases.forEach(purchase => {
            const date = new Date(purchase.purchaseDate);
            const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

            const existing = stats.get(monthKey) || 0;
            stats.set(monthKey, existing + (purchase.price ?? 0) * purchase.quantity);
        });

        return Array.from(stats.entries())
            .sort((a, b) => a[0].localeCompare(b[0]))
            .map(([key, value]) => {
                const [year, month] = key.split('-');
                const monthName = new Date(parseInt(year), parseInt(month) - 1).toLocaleDateString('ru-RU', { year: 'numeric', month: 'short' });
                return { month: monthName, amount: value };
            });
    }, [purchases]);

    const COLORS = ['#2d5016', '#4a7c2c', '#7fa650', '#8b7355', '#a4917a', '#c4b5a0'];

    // Получить список уникальных категорий
    const categories = useMemo(() => {
        const cats = new Set<string>();
        purchases.forEach(purchase => {
            if (purchase.goods?.category?.name) {
                cats.add(purchase.goods.category.name);
            }
        });
        return Array.from(cats).sort();
    }, [purchases]);

    // Фильтрация и сортировка
    const filteredAndSortedPurchases = useMemo(() => {
        let filtered = [...purchases];

        // Фильтрация по категории
        if (categoryFilter !== 'all') {
            filtered = filtered.filter(p => p.goods?.category?.name === categoryFilter);
        }

        // Сортировка
        filtered.sort((a, b) => {
            let comparison = 0;

            switch (sortField) {
                case 'date':
                    comparison = new Date(a.purchaseDate).getTime() - new Date(b.purchaseDate).getTime();
                    break;
                case 'price':
                    comparison = ((a.price ?? 0) * a.quantity) - ((b.price ?? 0) * b.quantity);
                    break;
                case 'name':
                    const nameA = a.goods?.name || '';
                    const nameB = b.goods?.name || '';
                    comparison = nameA.localeCompare(nameB, 'ru');
                    break;
            }

            return sortOrder === 'asc' ? comparison : -comparison;
        });

        return filtered;
    }, [purchases, categoryFilter, sortField, sortOrder]);

    // Пагинация
    const totalPages = Math.ceil(filteredAndSortedPurchases.length / itemsPerPage);
    const paginatedPurchases = useMemo(() => {
        const startIndex = (currentPage - 1) * itemsPerPage;
        return filteredAndSortedPurchases.slice(startIndex, startIndex + itemsPerPage);
    }, [filteredAndSortedPurchases, currentPage, itemsPerPage]);

    // Сброс текущей страницы при изменении фильтров
    useEffect(() => {
        setCurrentPage(1);
    }, [categoryFilter, sortField, sortOrder]);

    // Экспорт в CSV
    const exportToCSV = () => {
        const headers = ['Дата', 'Товар', 'Категория', 'Количество', 'Цена', 'Сумма'];
        const rows = filteredAndSortedPurchases.map(purchase => {
            const isCustom = purchase.kind === 'CUSTOM_FLORARIUM';
            const name = purchase.goods?.name || (isCustom ? 'Кастомный флорариум' : `ID: ${purchase.goodsId}`);
            const priceStr = purchase.price != null ? purchase.price.toFixed(2) : 'уточняется';
            const sumStr = purchase.price != null ? (purchase.price * purchase.quantity).toFixed(2) : 'уточняется';
            return [
                formatDate(purchase.purchaseDate),
                name,
                purchase.goods?.category?.name || (isCustom ? 'Кастомный флорариум' : '-'),
                purchase.quantity,
                priceStr,
                sumStr
            ];
        });

        const csvContent = [
            headers.join(','),
            ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
        ].join('\n');

        const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `purchases_${new Date().toISOString().split('T')[0]}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    if (authLoading || loading) {
        return <LoadingSpinner />;
    }

    return (
        <Container className="content-section">
            <h2 className="mb-4">Личный кабинет</h2>

            <Row>
                <Col md={4}>
                    <Card className="mb-4">
                        <Card.Body>
                            <Card.Title>Информация о пользователе</Card.Title>
                            <p><strong>Имя:</strong> {user?.name}</p>
                            <p><strong>Email:</strong> {user?.email}</p>
                        </Card.Body>
                    </Card>

                    <Card>
                        <Card.Body>
                            <Card.Title>Статистика</Card.Title>
                            <p><strong>Всего покупок:</strong> {purchases.length}</p>
                            <p><strong>Общая сумма:</strong> {totalAmount.toFixed(2)} ₽</p>
                            {categoryStats.length > 0 && (
                                <>
                                    <hr />
                                    <h6>Топ категорий:</h6>
                                    <ul className="list-unstyled">
                                        {categoryStats.slice(0, 3).map((cat, index) => (
                                            <li key={cat.name}>
                                                {index + 1}. {cat.name} - {cat.count} шт. ({cat.total.toFixed(2)} ₽)
                                            </li>
                                        ))}
                                    </ul>
                                </>
                            )}
                        </Card.Body>
                    </Card>
                </Col>

                <Col md={8}>
                    <Card>
                        <Card.Body>
                            <div className="d-flex flex-column flex-sm-row justify-content-between align-items-stretch align-items-sm-center gap-2 mb-3">
                                <Card.Title className="mb-0">История покупок</Card.Title>
                                {purchases.length > 0 && (
                                    <Button variant="success" size="sm" onClick={exportToCSV}>
                                        📥 Экспорт CSV
                                    </Button>
                                )}
                            </div>

                            {error && (
                                <Alert variant="danger">{error}</Alert>
                            )}

                            {purchases.length === 0 ? (
                                <Alert variant="info">
                                    У вас пока нет покупок. Посетите наш <Link to="/catalog">каталог</Link>!
                                </Alert>
                            ) : (
                                <>
                                    {/* Фильтры и сортировка */}
                                    <Row className="mb-3">
                                        <Col md={4}>
                                            <Form.Group>
                                                <Form.Label>Категория</Form.Label>
                                                <Form.Select
                                                    value={categoryFilter}
                                                    onChange={(e) => setCategoryFilter(e.target.value)}
                                                >
                                                    <option value="all">Все категории</option>
                                                    {categories.map(cat => (
                                                        <option key={cat} value={cat}>{cat}</option>
                                                    ))}
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                        <Col md={4}>
                                            <Form.Group>
                                                <Form.Label>Сортировать по</Form.Label>
                                                <Form.Select
                                                    value={sortField}
                                                    onChange={(e) => setSortField(e.target.value as SortField)}
                                                >
                                                    <option value="date">Дате</option>
                                                    <option value="price">Сумме</option>
                                                    <option value="name">Названию</option>
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                        <Col md={4}>
                                            <Form.Group>
                                                <Form.Label>Порядок</Form.Label>
                                                <Form.Select
                                                    value={sortOrder}
                                                    onChange={(e) => setSortOrder(e.target.value as SortOrder)}
                                                >
                                                    <option value="desc">По убыванию</option>
                                                    <option value="asc">По возрастанию</option>
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <p className="text-muted">
                                        Показано {paginatedPurchases.length} из {filteredAndSortedPurchases.length} покупок
                                    </p>

                                    <Table striped bordered hover className="rtable">
                                        <thead>
                                            <tr>
                                                <th>Дата</th>
                                                <th>Товар</th>
                                                <th>Количество</th>
                                                <th>Цена</th>
                                                <th>Сумма</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {paginatedPurchases.map((purchase) => {
                                                const isCustom = purchase.kind === 'CUSTOM_FLORARIUM';
                                                return (
                                                    <tr key={purchase.id}>
                                                        <td data-label="Дата">{formatDate(purchase.purchaseDate)}</td>
                                                        <td data-label="Товар">
                                                            {purchase.goods ? (
                                                                <Link
                                                                    to={`/catalog/${purchase.goodsId}`}
                                                                    className="text-decoration-none"
                                                                >
                                                                    {purchase.goods.name}
                                                                </Link>
                                                            ) : isCustom ? (
                                                                <span>
                                                                    🪴 Кастомный флорариум
                                                                    {purchase.status && (
                                                                        <span className="badge bg-secondary ms-2">
                                                                            {customOrderStatusLabel(purchase.status)}
                                                                        </span>
                                                                    )}
                                                                </span>
                                                            ) : (
                                                                <span className="text-muted">ID: {purchase.goodsId}</span>
                                                            )}
                                                        </td>
                                                        <td data-label="Количество">{purchase.quantity}</td>
                                                        <td data-label="Цена">
                                                            {purchase.price != null
                                                                ? `${purchase.price.toFixed(2)} ₽`
                                                                : <span className="text-muted">Уточняется</span>}
                                                        </td>
                                                        <td data-label="Сумма">
                                                            {purchase.price != null
                                                                ? `${(purchase.price * purchase.quantity).toFixed(2)} ₽`
                                                                : <span className="text-muted">—</span>}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </Table>

                                    {/* Пагинация */}
                                    {totalPages > 1 && (
                                        <div className="d-flex justify-content-center">
                                            <Pagination size="sm" className="flex-wrap justify-content-center">
                                                <Pagination.First
                                                    onClick={() => setCurrentPage(1)}
                                                    disabled={currentPage === 1}
                                                />
                                                <Pagination.Prev
                                                    onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                                                    disabled={currentPage === 1}
                                                />

                                                {[...Array(totalPages)].map((_, index) => {
                                                    const page = index + 1;
                                                    // Показываем только соседние страницы
                                                    if (
                                                        page === 1 ||
                                                        page === totalPages ||
                                                        (page >= currentPage - 1 && page <= currentPage + 1)
                                                    ) {
                                                        return (
                                                            <Pagination.Item
                                                                key={page}
                                                                active={page === currentPage}
                                                                onClick={() => setCurrentPage(page)}
                                                            >
                                                                {page}
                                                            </Pagination.Item>
                                                        );
                                                    } else if (page === currentPage - 2 || page === currentPage + 2) {
                                                        return <Pagination.Ellipsis key={page} disabled />;
                                                    }
                                                    return null;
                                                })}

                                                <Pagination.Next
                                                    onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                                                    disabled={currentPage === totalPages}
                                                />
                                                <Pagination.Last
                                                    onClick={() => setCurrentPage(totalPages)}
                                                    disabled={currentPage === totalPages}
                                                />
                                            </Pagination>
                                        </div>
                                    )}

                                    <div className="mt-3 text-end">
                                        <strong>Итого ({filteredAndSortedPurchases.length} {purchasesWord(filteredAndSortedPurchases.length)}): {
                                            filteredAndSortedPurchases.reduce((sum, p) => sum + (p.price ?? 0) * p.quantity, 0).toFixed(2)
                                        } ₽</strong>
                                    </div>
                                </>
                            )}
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {purchases.length > 0 && (
                <Row className="mt-4">
                    <Col md={6}>
                        <Card>
                            <Card.Body>
                                <Card.Title>Покупки по месяцам</Card.Title>
                                {monthlyStats.length > 0 ? (
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={monthlyStats}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis dataKey="month" />
                                            <YAxis />
                                            <Tooltip formatter={(value) => `${Number(value).toFixed(2)} ₽`} />
                                            <Legend />
                                            <Bar dataKey="amount" fill="#4a7c2c" name="Сумма покупок" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                ) : (
                                    <p className="text-muted">Недостаточно данных для отображения</p>
                                )}
                            </Card.Body>
                        </Card>
                    </Col>

                    <Col md={6}>
                        <Card>
                            <Card.Body>
                                <Card.Title>Распределение по категориям</Card.Title>
                                {categoryStats.length > 0 ? (
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={categoryStats}
                                                dataKey="total"
                                                nameKey="name"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={isMobile ? 70 : 80}
                                                label={isMobile ? false : (props: any) => `${props.name}: ${props.value.toFixed(0)} ₽`}
                                            >
                                                {categoryStats.map((_, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip formatter={(value) => `${Number(value).toFixed(2)} ₽`} />
                                            {isMobile && <Legend />}
                                        </PieChart>
                                    </ResponsiveContainer>
                                ) : (
                                    <p className="text-muted">Недостаточно данных для отображения</p>
                                )}
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            )}
        </Container>
    );
}

export default ProfilePage;
