import React, { useEffect } from 'react';
import { Container, Table, Button, Badge, Card } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { toast } from 'react-toastify';
import { cartService } from '../services/cartService';

const CartPage: React.FC = () => {
    const navigate = useNavigate();
    const { user, isAuthenticated, loading: authLoading } = useAuth();
    const { cart, localCartItems, loading, updateQuantity, removeItem, clearCart, refreshCart } = useCart();

    useEffect(() => {
        // Ждём завершения проверки аутентификации перед загрузкой корзины
        if (!authLoading)
            refreshCart();
    }, [authLoading]);

    if (loading || authLoading)
        return <Container style={{ paddingTop: '3rem' }}><p>Загрузка корзины...</p></Container>;

    // Определяем какую корзину отображать
    const displayCart = isAuthenticated
        ? cart
        : (localCartItems.length > 0 ? {
            items: localCartItems,
            totalItems: localCartItems.reduce((sum, item) => sum + item.quantity, 0),
            totalPrice: localCartItems.reduce((sum, item) => sum + (item.goods.price * item.quantity), 0)
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
                            onClick={() => navigate('/catalog')}
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

    const handleClear = async () => {
        if (window.confirm('Вы уверены, что хотите очистить корзину?')) {
            try {
                await clearCart();
                toast.success('Корзина очищена');
            } catch (error) {
                toast.error('Ошибка при очистке корзины');
            }
        }
    };

    const handleCheckout = async () => {
        try {
            const response = await cartService.checkout();
            toast.success(`Заказ №${response.orderId.substring(0, 8)} успешно оформлен!`);
            toast.info(`Итого: ${response.totalPrice.toFixed(0)} ₽ (${response.items.length} товаров)`);

            await refreshCart();
            // Можно показать модальное окно с деталями заказа, но для простоты просто перенаправляем
            navigate('/catalog');
        } catch (error: any) {
            console.error('Checkout error:', error);
            const errorMessage = error.response?.data?.message || 'Ошибка при оформлении заказа';
            toast.error(errorMessage);
        }
    };

    // Корзина с товарами
    return (
        <Container className="cart-page" style={{ paddingTop: '2rem', paddingBottom: '3rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                <h2 style={{ color: 'var(--forest-green)' }}>🛒 Ваша корзина</h2>
                <Button variant="outline-secondary" onClick={() => navigate('/catalog')}>
                    Продолжить покупки
                </Button>
            </div>

            <Table striped bordered hover responsive className="cart-table">
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
                    {displayCart.items.map(item => (
                        <tr key={item.id}>
                            <td>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                    <img
                                        src={item.goods.media.find(m => m.type === 'image')?.url || item.goods.media[0]?.url || ''}
                                        alt={item.goods.name}
                                        style={{
                                            width: '60px',
                                            height: '60px',
                                            objectFit: 'cover',
                                            borderRadius: '8px',
                                            boxShadow: '0 2px 8px var(--shadow)'
                                        }}
                                    />
                                    <div>
                                        <strong>{item.goods.name}</strong>
                                        <div className="text-muted" style={{ fontSize: '0.85rem' }}>
                                            {item.goods.category?.name || 'Без категории'}
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td style={{ verticalAlign: 'middle' }}>
                                <strong>{item.goods.price.toFixed(0)} ₽</strong>
                            </td>
                            <td style={{ verticalAlign: 'middle' }}>
                                {item.goods.category?.type === 'COURSE' ? (
                                    <div style={{ textAlign: 'center' }}>
                                        <span style={{ fontWeight: 'bold' }}>1 шт</span>
                                    </div>
                                ) : (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'center' }}>
                                        <Button
                                            size="sm"
                                            variant="outline-secondary"
                                            onClick={() => updateQuantity(item.goods.id, Math.max(1, item.quantity - 1))}
                                            disabled={loading || item.quantity <= 1}
                                            title={item.quantity <= 1 ? 'Минимум 1' : ''}
                                        >
                                            −
                                        </Button>
                                        <span style={{ minWidth: '30px', textAlign: 'center', fontWeight: 'bold' }}>
                                            {item.quantity}
                                        </span>
                                        <Button
                                            size="sm"
                                            variant="outline-secondary"
                                            onClick={() => updateQuantity(item.goods.id, Math.min(99, item.quantity + 1))}
                                            disabled={loading || item.quantity >= 99}
                                            title={item.quantity >= 99 ? 'Максимум 99' : ''}
                                        >
                                            +
                                        </Button>
                                    </div>
                                )}
                            </td>
                            <td style={{ verticalAlign: 'middle' }}>
                                <strong style={{ color: 'var(--sage-green)', fontSize: '1.1rem' }}>
                                    {(item.goods.price * item.quantity).toFixed(0)} ₽
                                </strong>
                            </td>
                            <td style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                                <Button
                                    size="sm"
                                    variant="outline-danger"
                                    onClick={() => handleRemove(item.goods.id, item.goods.name)}
                                    disabled={loading}
                                >
                                    ✕
                                </Button>
                            </td>
                        </tr>
                    ))}
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

                <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem' }}>
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
                            onClick={handleCheckout}
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
        </Container>
    );
};

export default CartPage;
