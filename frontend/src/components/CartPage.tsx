import React, { useEffect, useState, useRef } from 'react';
import { Container, Table, Button, Badge, Card, Modal } from 'react-bootstrap';
import { Dash, Plus, Trash } from 'react-bootstrap-icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { toast } from 'react-toastify';
import { aiBotService } from '../services/aiBotService';
import LazyImage from './LazyImage';
import LoadingSpinner from './LoadingSpinner';

const CartPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, isAuthenticated, loading: authLoading } = useAuth();
    const { cart, localCartItems, loading, updateQuantity, removeItem, removeCustomItem, clearCart, refreshCart } = useCart();

    // Превью картинок кастомных флорариумов (imageUrl -> blob objectURL, грузим авторизованным запросом)
    const [customImages, setCustomImages] = useState<Record<string, string>>({});
    const [showClearModal, setShowClearModal] = useState(false);
    const customObjectUrls = useRef<string[]>([]);

    useEffect(() => {
        // Ждём завершения проверки аутентификации перед загрузкой корзины
        if (!authLoading)
            refreshCart();
    }, [authLoading]);

    // Догрузить картинки кастомных элементов (только серверная корзина)
    useEffect(() => {
        (cart?.items ?? []).forEach(item => {
            const url = item.imageUrl;
            if (!item.goods && url && !customImages[url]) {
                aiBotService.fetchFlorariumImage(url)
                    .then(blob => {
                        customObjectUrls.current.push(blob);
                        setCustomImages(prev => ({ ...prev, [url]: blob }));
                    })
                    .catch(() => { /* картинка недоступна — покажем плейсхолдер */ });
            }
        });
    }, [cart]);

    // Освободить blob-URL при размонтировании
    useEffect(() => () => {
        customObjectUrls.current.forEach(u => URL.revokeObjectURL(u));
        customObjectUrls.current = [];
    }, []);

    // Функция для определения пути возврата из корзины
    const getReturnPath = (): string => {
        // Проверяем state из navigation (откуда пришли)
        const state = location.state as { from?: string; categoryType?: string } | null;

        if (state?.from) {
            // Если пришли из конкретной страницы, возвращаем на соответствующий каталог
            if (state.from.startsWith('/catalog/') && state.categoryType) {
                // Пришли со страницы товара - вернуться в каталог по типу категории
                switch (state.categoryType) {
                    case 'PLANT':
                        return '/catalog';
                    case 'TERRARIUM':
                        return '/terrariums';
                    case 'COURSE':
                        return '/masterclasses';
                    default:
                        return '/catalog';
                }
            }
            // Иначе возвращаем на ту страницу, откуда пришли
            return state.from;
        }

        // По умолчанию - каталог растений
        return '/catalog';
    };

    const handleContinueShopping = () => {
        navigate(getReturnPath());
    };

    if (loading || authLoading)
        return <LoadingSpinner text="Загрузка корзины..." />;

    // Определяем какую корзину отображать
    const displayCart = isAuthenticated
        ? cart
        : (localCartItems.length > 0 ? {
            items: localCartItems,
            totalItems: localCartItems.reduce((sum, item) => sum + item.quantity, 0),
            totalPrice: localCartItems.reduce((sum, item) => sum + ((item.goods?.price ?? 0) * item.quantity), 0)
        } : null);

    // Пустая корзина
    if (!displayCart || displayCart.items.length === 0) {
        return (
            <Container className="cart-page" style={{ paddingTop: '3rem', textAlign: 'center' }}>
                <Card className="auth-card" style={{ margin: '0 auto' }}>
                    <Card.Body>
                        <div className="cart-icon" style={{ fontSize: '4rem' }}>🛒</div>
                        <h2>Корзина пуста</h2>
                        <p className="text-muted">Добавьте растения из каталога</p>
                        <Button
                            variant="success"
                            onClick={handleContinueShopping}
                            style={{ marginTop: '1rem' }}
                        >
                            Перейти в каталог 🌿
                        </Button>
                    </Card.Body>
                </Card>
            </Container>
        );
    }

    const handleRemove = async (goodsId: string, goodsName: string) => {
        try {
            await removeItem(goodsId);
            toast.info(`${goodsName} удалено из корзины`);
        } catch (error) {
            toast.error('Ошибка при удалении товара');
        }
    };

    const handleRemoveCustom = async (id: string) => {
        try {
            await removeCustomItem(id);
            toast.info('Заказ флорариума удалён из корзины');
        } catch (error) {
            toast.error('Ошибка при удалении заказа');
        }
    };

    const handleClear = () => setShowClearModal(true);

    const confirmClear = async () => {
        setShowClearModal(false);
        try {
            await clearCart();
            toast.success('Корзина очищена');
        } catch (error) {
            toast.error('Ошибка при очистке корзины');
        }
    };

    // Корзина с товарами
    return (
        <Container className="cart-page" style={{ paddingTop: '2rem', paddingBottom: '3rem' }}>
            <div className="toolbar" style={{ marginBottom: '2rem' }}>
                <h2 style={{ color: 'var(--forest-green)', margin: 0 }}>🛒 Ваша корзина</h2>
                <Button variant="outline-secondary" onClick={handleContinueShopping}>
                    Продолжить покупки
                </Button>
            </div>

            <Table striped bordered hover className="cart-table rtable">
                <thead style={{ backgroundColor: 'var(--light-green)' }}>
                    <tr>
                        <th>Растение</th>
                        <th style={{ width: '120px' }}>Цена</th>
                        <th style={{ width: '160px' }}>Количество</th>
                        <th style={{ width: '120px' }}>Сумма</th>
                        <th style={{ width: '100px' }}>Действия</th>
                    </tr>
                </thead>
                <tbody>
                    {displayCart.items.map(item => {
                        const goods = item.goods;

                        // Кастомный флорариум: нет товара каталога, цена уточняется
                        if (!goods) {
                            const preview = item.imageUrl ? customImages[item.imageUrl] : undefined;
                            return (
                                <tr key={item.id}>
                                    <td>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                            {preview ? (
                                                <img
                                                    src={preview}
                                                    alt="Кастомный флорариум"
                                                    style={{ width: '60px', height: '60px', objectFit: 'cover', borderRadius: '8px', boxShadow: '0 2px 8px var(--shadow)' }}
                                                />
                                            ) : (
                                                <div style={{ width: '60px', height: '60px', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--light-green)', borderRadius: '8px', fontSize: '1.6rem' }}>
                                                    🪴
                                                </div>
                                            )}
                                            <div>
                                                <strong>Кастомный флорариум</strong>
                                                <div className="text-muted" style={{ fontSize: '0.85rem' }}>Индивидуальный заказ</div>
                                                {item.customerComment && (
                                                    <div className="text-muted" style={{ fontSize: '0.8rem', fontStyle: 'italic' }}>
                                                        «{item.customerComment}»
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </td>
                                    <td data-label="Цена" style={{ verticalAlign: 'middle' }}>
                                        <span className="text-muted">Уточняется</span>
                                    </td>
                                    <td data-label="Количество" style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                                        <span style={{ fontWeight: 'bold' }}>1 шт</span>
                                    </td>
                                    <td data-label="Сумма" style={{ verticalAlign: 'middle' }}>
                                        <span className="text-muted">—</span>
                                    </td>
                                    <td data-label="Действия" style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                                        <Button
                                            size="sm"
                                            variant="outline-danger"
                                            onClick={() => handleRemoveCustom(item.id)}
                                            disabled={loading}
                                            aria-label="Удалить заказ флорариума из корзины"
                                        >
                                            <Trash aria-hidden="true" />
                                        </Button>
                                    </td>
                                </tr>
                            );
                        }

                        // Обычный товар каталога
                        return (
                            <tr key={item.id}>
                                <td>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                        <LazyImage
                                            src={goods.media.find(m => m.type === 'image')?.url || goods.media[0]?.url || ''}
                                            alt={goods.name}
                                            style={{
                                                width: '60px',
                                                height: '60px',
                                                objectFit: 'cover',
                                                borderRadius: '8px',
                                                boxShadow: '0 2px 8px var(--shadow)'
                                            }}
                                            showLoader={false}
                                        />
                                        <div>
                                            <strong>{goods.name}</strong>
                                            <div className="text-muted" style={{ fontSize: '0.85rem' }}>
                                                {goods.category?.name || 'Без категории'}
                                            </div>
                                        </div>
                                    </div>
                                </td>
                                <td data-label="Цена" style={{ verticalAlign: 'middle' }}>
                                    <strong>{goods.price.toFixed(0)} ₽</strong>
                                </td>
                                <td data-label="Количество" style={{ verticalAlign: 'middle' }}>
                                    {goods.category?.type === 'COURSE' ? (
                                        <div style={{ textAlign: 'center' }}>
                                            <span style={{ fontWeight: 'bold' }}>1 шт</span>
                                        </div>
                                    ) : (
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'center' }}>
                                            <Button
                                                size="sm"
                                                variant="outline-secondary"
                                                onClick={() => updateQuantity(goods.id, Math.max(1, item.quantity - 1))}
                                                disabled={loading || item.quantity <= 1}
                                                title={item.quantity <= 1 ? 'Минимум 1' : ''}
                                                aria-label="Уменьшить количество"
                                            >
                                                <Dash aria-hidden="true" />
                                            </Button>
                                            <span style={{ minWidth: '30px', textAlign: 'center', fontWeight: 'bold' }}>
                                                {item.quantity}
                                            </span>
                                            <Button
                                                size="sm"
                                                variant="outline-secondary"
                                                onClick={() => updateQuantity(goods.id, Math.min(99, item.quantity + 1))}
                                                disabled={loading || item.quantity >= 99}
                                                title={item.quantity >= 99 ? 'Максимум 99' : ''}
                                                aria-label="Увеличить количество"
                                            >
                                                <Plus aria-hidden="true" />
                                            </Button>
                                        </div>
                                    )}
                                </td>
                                <td data-label="Сумма" style={{ verticalAlign: 'middle' }}>
                                    <strong style={{ color: 'var(--sage-green)', fontSize: '1.1rem' }}>
                                        {(goods.price * item.quantity).toFixed(0)} ₽
                                    </strong>
                                </td>
                                <td data-label="Действия" style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                                    <Button
                                        size="sm"
                                        variant="outline-danger"
                                        onClick={() => handleRemove(goods.id, goods.name)}
                                        disabled={loading}
                                        aria-label={`Удалить ${goods.name} из корзины`}
                                    >
                                        <Trash aria-hidden="true" />
                                    </Button>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </Table>

            <Card className="cart-summary-card" style={{ marginTop: '2rem', padding: '1.5rem', backgroundColor: 'var(--light-green)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                        <h4 style={{ color: 'var(--forest-green)', marginBottom: '0.5rem' }}>Итого</h4>
                        <p className="text-muted" style={{ marginBottom: 0 }}>
                            Товаров: <Badge bg="info">{displayCart.totalItems}</Badge>
                        </p>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--forest-green)' }}>
                            {displayCart.totalPrice.toFixed(0)} ₽
                        </div>
                    </div>
                </div>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', marginTop: '1.5rem' }}>
                    <Button
                        variant="outline-danger"
                        onClick={handleClear}
                        disabled={loading}
                    >
                        Очистить корзину
                    </Button>
                    {user?.canPurchase ? (
                        <Button
                            variant="success"
                            onClick={() => navigate('/checkout')}
                            style={{ flex: 1 }}
                        >
                            Оформить заказ ✓
                        </Button>
                    ) : isAuthenticated ? (
                        <Button
                            variant="warning"
                            disabled
                            style={{ flex: 1 }}
                        >
                            У вас нет прав на покупку 🚫
                        </Button>
                    ) : (
                        <Button
                            variant="success"
                            onClick={() => navigate('/login', { state: { from: '/cart' } })}
                            style={{ flex: 1 }}
                        >
                            Войдите для оформления заказа 🌿
                        </Button>
                    )}
                </div>
            </Card>

            <Modal show={showClearModal} onHide={() => setShowClearModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>Очистить корзину?</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    Все товары будут удалены из корзины. Это действие нельзя отменить.
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowClearModal(false)}>
                        Отмена
                    </Button>
                    <Button variant="danger" onClick={confirmClear}>
                        Очистить
                    </Button>
                </Modal.Footer>
            </Modal>
        </Container>
    );
};

export default CartPage;
