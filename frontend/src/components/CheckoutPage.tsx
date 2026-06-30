import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Table, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { cartService } from '../services/cartService';
import { toast } from 'react-toastify';

type DeliveryMethod = 'courier' | 'pickup';

const CheckoutPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { cart, refreshCart } = useCart();

    const [name, setName] = useState(user?.name ?? '');
    const [phone, setPhone] = useState('');
    const [delivery, setDelivery] = useState<DeliveryMethod>('courier');
    const [address, setAddress] = useState('');
    const [comment, setComment] = useState('');
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [submitting, setSubmitting] = useState(false);

    const items = cart?.items ?? [];
    const totalItems = cart?.totalItems ?? 0;
    const totalPrice = cart?.totalPrice ?? 0;

    // Корзина пуста — нечего оформлять
    if (items.length === 0) {
        return (
            <Container className="checkout-page" style={{ paddingTop: '3rem', textAlign: 'center' }}>
                <Card className="auth-card" style={{ margin: '0 auto' }}>
                    <Card.Body>
                        <div style={{ fontSize: '3rem' }} aria-hidden="true">🛒</div>
                        <h2>Корзина пуста</h2>
                        <p className="text-muted">Добавьте товары, чтобы оформить заказ.</p>
                        <Button variant="success" onClick={() => navigate('/catalog')}>Перейти в каталог 🌿</Button>
                    </Card.Body>
                </Card>
            </Container>
        );
    }

    // Нет прав на покупку
    if (!user?.canPurchase) {
        return (
            <Container className="checkout-page" style={{ paddingTop: '3rem' }}>
                <Alert variant="warning" style={{ maxWidth: 560, margin: '0 auto', textAlign: 'center' }}>
                    У вашего аккаунта нет прав на покупку. Обратитесь к администратору.
                </Alert>
            </Container>
        );
    }

    const validate = (): boolean => {
        const e: Record<string, string> = {};
        if (name.trim().length < 2) e.name = 'Укажите имя получателя';
        if (phone.replace(/\D/g, '').length < 10) e.phone = 'Укажите корректный номер телефона';
        if (delivery === 'courier' && address.trim().length < 5) e.address = 'Укажите адрес доставки';
        setErrors(e);
        return Object.keys(e).length === 0;
    };

    const handleSubmit = async (ev: React.FormEvent) => {
        ev.preventDefault();
        if (!validate()) return;

        setSubmitting(true);
        try {
            const order = await cartService.checkout();
            await refreshCart();
            navigate('/order-confirmation', {
                replace: true,
                state: {
                    order,
                    delivery: { name, phone, method: delivery, address, comment }
                }
            });
        } catch (error: any) {
            const message = error.response?.data?.message || 'Не удалось оформить заказ. Попробуйте ещё раз.';
            toast.error(message);
            setSubmitting(false);
        }
    };

    return (
        <Container className="checkout-page" style={{ paddingTop: '2rem', paddingBottom: '3rem' }}>
            <h1 style={{ color: 'var(--forest-green)', marginBottom: '1.5rem' }}>Оформление заказа</h1>

            <Row className="g-4">
                <Col xs={12} lg={7}>
                    <Card className="checkout-card">
                        <Card.Body>
                            <h2 className="h5 mb-3">Контакты и доставка</h2>
                            <Form onSubmit={handleSubmit} noValidate>
                                <Form.Group className="mb-3" controlId="checkout-name">
                                    <Form.Label>Имя получателя</Form.Label>
                                    <Form.Control
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        isInvalid={!!errors.name}
                                    />
                                    <Form.Control.Feedback type="invalid">{errors.name}</Form.Control.Feedback>
                                </Form.Group>

                                <Form.Group className="mb-3" controlId="checkout-phone">
                                    <Form.Label>Телефон</Form.Label>
                                    <Form.Control
                                        type="tel"
                                        value={phone}
                                        onChange={(e) => setPhone(e.target.value)}
                                        placeholder="+7 999 123-45-67"
                                        isInvalid={!!errors.phone}
                                    />
                                    <Form.Control.Feedback type="invalid">{errors.phone}</Form.Control.Feedback>
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>Способ получения</Form.Label>
                                    <Form.Check
                                        type="radio"
                                        name="delivery"
                                        id="delivery-courier"
                                        label="Доставка курьером"
                                        checked={delivery === 'courier'}
                                        onChange={() => setDelivery('courier')}
                                    />
                                    <Form.Check
                                        type="radio"
                                        name="delivery"
                                        id="delivery-pickup"
                                        label="Самовывоз из магазина"
                                        checked={delivery === 'pickup'}
                                        onChange={() => setDelivery('pickup')}
                                    />
                                </Form.Group>

                                {delivery === 'courier' && (
                                    <Form.Group className="mb-3" controlId="checkout-address">
                                        <Form.Label>Адрес доставки</Form.Label>
                                        <Form.Control
                                            value={address}
                                            onChange={(e) => setAddress(e.target.value)}
                                            placeholder="Город, улица, дом, квартира"
                                            isInvalid={!!errors.address}
                                        />
                                        <Form.Control.Feedback type="invalid">{errors.address}</Form.Control.Feedback>
                                    </Form.Group>
                                )}

                                <Form.Group className="mb-3" controlId="checkout-comment">
                                    <Form.Label>Комментарий к заказу <span className="text-muted">(необязательно)</span></Form.Label>
                                    <Form.Control
                                        as="textarea"
                                        rows={2}
                                        value={comment}
                                        onChange={(e) => setComment(e.target.value)}
                                    />
                                </Form.Group>

                                <div className="d-flex gap-2 flex-wrap">
                                    <Button variant="outline-secondary" type="button" onClick={() => navigate('/cart')}>
                                        Назад в корзину
                                    </Button>
                                    <Button variant="success" type="submit" disabled={submitting} style={{ flex: 1 }}>
                                        {submitting ? 'Оформляем…' : `Подтвердить заказ — ${totalPrice.toFixed(0)} ₽`}
                                    </Button>
                                </div>
                            </Form>
                        </Card.Body>
                    </Card>
                </Col>

                <Col xs={12} lg={5}>
                    <Card className="checkout-summary" style={{ backgroundColor: 'var(--light-green)' }}>
                        <Card.Body>
                            <h2 className="h5 mb-3">Ваш заказ</h2>
                            <Table borderless size="sm" className="mb-2">
                                <tbody>
                                    {items.map((item) => (
                                        <tr key={item.id}>
                                            <td>
                                                {item.goods ? item.goods.name : 'Кастомный флорариум'}{' '}
                                                <span className="text-muted">× {item.quantity}</span>
                                            </td>
                                            <td className="text-end">
                                                {item.goods ? `${(item.goods.price * item.quantity).toFixed(0)} ₽` : 'Уточняется'}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </Table>
                            <hr />
                            <div className="d-flex justify-content-between align-items-center">
                                <span>Товаров: {totalItems}</span>
                                <strong style={{ fontSize: '1.4rem', color: 'var(--forest-green)' }}>
                                    {totalPrice.toFixed(0)} ₽
                                </strong>
                            </div>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
};

export default CheckoutPage;
