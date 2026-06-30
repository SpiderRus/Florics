import React from 'react';
import { Container, Card, Button, Table, Badge } from 'react-bootstrap';
import { useLocation, useNavigate } from 'react-router-dom';
import { CheckoutResponse } from '../types/cart';

interface DeliveryInfo {
    name: string;
    phone: string;
    method: 'courier' | 'pickup';
    address: string;
    comment: string;
}

const OrderConfirmation: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const state = location.state as { order?: CheckoutResponse; delivery?: DeliveryInfo } | null;
    const order = state?.order;
    const delivery = state?.delivery;

    // Прямой заход без данных заказа
    if (!order) {
        return (
            <Container style={{ paddingTop: '3rem', textAlign: 'center' }}>
                <Card className="auth-card" style={{ margin: '0 auto' }}>
                    <Card.Body>
                        <div style={{ fontSize: '3rem' }} aria-hidden="true">🌿</div>
                        <h2>Заказ не найден</h2>
                        <p className="text-muted">
                            Похоже, страница открыта напрямую. Историю заказов можно посмотреть в личном кабинете.
                        </p>
                        <div className="d-flex gap-2 justify-content-center flex-wrap">
                            <Button variant="outline-secondary" onClick={() => navigate('/profile')}>Личный кабинет</Button>
                            <Button variant="success" onClick={() => navigate('/catalog')}>В каталог 🌿</Button>
                        </div>
                    </Card.Body>
                </Card>
            </Container>
        );
    }

    const date = new Date(order.purchaseDate).toLocaleString('ru-RU', { dateStyle: 'long', timeStyle: 'short' });

    return (
        <Container className="order-confirmation" style={{ paddingTop: '2.5rem', paddingBottom: '3rem', maxWidth: 720 }}>
            <Card className="checkout-card">
                <Card.Body style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '3.5rem' }} aria-hidden="true">✅</div>
                    <h1 style={{ color: 'var(--forest-green)' }}>Заказ оформлен!</h1>
                    <p className="text-muted">Спасибо за покупку. Мы свяжемся с вами для подтверждения.</p>
                    <p className="mb-1">
                        Номер заказа:{' '}
                        <Badge bg="success">№ {order.orderId.substring(0, 8).toUpperCase()}</Badge>
                    </p>
                    <p className="text-muted">{date}</p>
                </Card.Body>
            </Card>

            <Card className="checkout-summary mt-3">
                <Card.Body>
                    <h2 className="h5 mb-3">Состав заказа</h2>
                    <Table borderless size="sm">
                        <tbody>
                            {order.items.map((it, idx) => (
                                <tr key={idx}>
                                    <td>{it.goodsName} <span className="text-muted">× {it.quantity}</span></td>
                                    <td className="text-end">
                                        {it.price != null ? `${(it.price * it.quantity).toFixed(0)} ₽` : 'Уточняется'}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                    <hr />
                    <div className="d-flex justify-content-between">
                        <span>Итого</span>
                        <strong style={{ color: 'var(--forest-green)', fontSize: '1.3rem' }}>
                            {order.totalPrice.toFixed(0)} ₽
                        </strong>
                    </div>
                </Card.Body>
            </Card>

            {delivery && (
                <Card className="checkout-summary mt-3">
                    <Card.Body>
                        <h2 className="h5 mb-3">Доставка</h2>
                        <p className="mb-1">{delivery.name} · {delivery.phone}</p>
                        <p className="mb-1">
                            {delivery.method === 'pickup'
                                ? 'Самовывоз из магазина'
                                : `Курьер: ${delivery.address}`}
                        </p>
                        {delivery.comment && <p className="text-muted mb-0">Комментарий: {delivery.comment}</p>}
                    </Card.Body>
                </Card>
            )}

            <div className="d-flex gap-2 justify-content-center flex-wrap mt-4">
                <Button variant="outline-secondary" onClick={() => navigate('/profile')}>Мои заказы</Button>
                <Button variant="success" onClick={() => navigate('/catalog')}>Продолжить покупки 🌿</Button>
            </div>
        </Container>
    );
};

export default OrderConfirmation;
