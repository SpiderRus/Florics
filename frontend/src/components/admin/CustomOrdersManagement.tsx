import React, { useEffect, useRef, useState } from 'react';
import { Table, Form, Button, Spinner, Alert, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { customOrderService } from '../../services/customOrderService';
import { aiBotService } from '../../services/aiBotService';
import { CustomOrder, CUSTOM_ORDER_STATUSES, customOrderStatusLabel } from '../../types/customOrder';
import ExpertChatModal from './ExpertChatModal';

interface RowDraft {
    price: string;
    status: string;
}

/**
 * Экран админки: управление кастомными заказами флорариумов.
 * Просмотр заказов (с превью выбранной картинки, комментарием и контактами),
 * проставление цены и смена статуса.
 */
export const CustomOrdersManagement: React.FC = () => {
    const [orders, setOrders] = useState<CustomOrder[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [savingId, setSavingId] = useState<string | null>(null);

    // Черновики редактирования по id заказа
    const [drafts, setDrafts] = useState<Record<string, RowDraft>>({});
    // Превью картинок (imageUrl -> blob objectURL)
    const [images, setImages] = useState<Record<string, string>>({});
    const objectUrls = useRef<string[]>([]);

    // Заказ, для которого открыт экспертный чат
    const [expertOrder, setExpertOrder] = useState<CustomOrder | null>(null);

    const loadOrders = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await customOrderService.getAllOrders();
            setOrders(data);
            setDrafts(
                Object.fromEntries(data.map(o => [o.id, {
                    price: o.price != null ? String(o.price) : '',
                    status: o.status ?? 'NEW'
                }]))
            );
        } catch (err) {
            console.error('Failed to load custom orders:', err);
            setError('Не удалось загрузить заказы');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadOrders();
    }, []);

    // Догрузить превью картинок заказов авторизованным запросом
    useEffect(() => {
        orders.forEach(o => {
            if (o.imageUrl && !images[o.imageUrl]) {
                aiBotService.fetchFlorariumImage(o.imageUrl)
                    .then(blob => {
                        objectUrls.current.push(blob);
                        setImages(prev => ({ ...prev, [o.imageUrl as string]: blob }));
                    })
                    .catch(() => { /* картинка недоступна — покажем плейсхолдер */ });
            }
        });
    }, [orders]);

    useEffect(() => () => {
        objectUrls.current.forEach(u => URL.revokeObjectURL(u));
        objectUrls.current = [];
    }, []);

    const formatDate = (d: string) =>
        new Date(d).toLocaleDateString('ru-RU', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

    const setDraft = (id: string, patch: Partial<RowDraft>) =>
        setDrafts(prev => ({ ...prev, [id]: { ...prev[id], ...patch } }));

    const handleSave = async (order: CustomOrder) => {
        const draft = drafts[order.id];
        if (!draft) return;

        const priceValue = draft.price.trim() === '' ? null : Number(draft.price);
        if (priceValue != null && (isNaN(priceValue) || priceValue < 0)) {
            toast.error('Некорректная цена');
            return;
        }

        try {
            setSavingId(order.id);
            const updated = await customOrderService.updateOrder(order.id, {
                price: priceValue,
                status: draft.status
            });
            setOrders(prev => prev.map(o => (o.id === updated.id ? updated : o)));
            toast.success('Заказ обновлён');
        } catch (err) {
            console.error('Failed to update order:', err);
            toast.error('Не удалось обновить заказ');
        } finally {
            setSavingId(null);
        }
    };

    if (loading) {
        return (
            <div className="text-center p-5">
                <Spinner animation="border" role="status">
                    <span className="visually-hidden">Загрузка...</span>
                </Spinner>
            </div>
        );
    }

    if (error) {
        return <Alert variant="danger">{error}</Alert>;
    }

    if (orders.length === 0) {
        return <div className="text-center text-muted p-5">Заказов кастомных флорариумов пока нет</div>;
    }

    return (
        <>
        <Table striped bordered hover responsive className="align-middle rtable">
            <thead>
                <tr>
                    <th style={{ width: 90 }}>Дизайн</th>
                    <th>Покупатель</th>
                    <th>Дата</th>
                    <th>Комментарий / контакты</th>
                    <th style={{ width: 150 }}>Статус</th>
                    <th style={{ width: 140 }}>Цена, ₽</th>
                    <th style={{ width: 200 }}>Действия</th>
                </tr>
            </thead>
            <tbody>
                {orders.map(order => {
                    const draft = drafts[order.id] ?? { price: '', status: 'NEW' };
                    const preview = order.imageUrl ? images[order.imageUrl] : undefined;
                    return (
                        <tr key={order.id}>
                            <td data-label="Дизайн">
                                {preview ? (
                                    <img
                                        src={preview}
                                        alt="Флорариум"
                                        style={{ width: 70, height: 70, objectFit: 'cover', borderRadius: 8 }}
                                    />
                                ) : (
                                    <div style={{ width: 70, height: 70, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--light-green, #eef3e8)', borderRadius: 8, fontSize: '1.6rem' }}>
                                        🪴
                                    </div>
                                )}
                            </td>
                            <td data-label="Покупатель" className="rtable-stack">
                                <div><strong>{order.userName || '—'}</strong></div>
                                <div className="text-muted small">{order.userEmail || ''}</div>
                            </td>
                            <td className="small" data-label="Дата">{formatDate(order.purchaseDate)}</td>
                            <td data-label="Комментарий / контакты" className="rtable-stack">
                                {order.customerComment
                                    ? <div className="small">«{order.customerComment}»</div>
                                    : <div className="text-muted small">без комментария</div>}
                                {order.contact && <div className="small mt-1">📞 {order.contact}</div>}
                                <Badge bg="light" text="dark" className="mt-1">{customOrderStatusLabel(order.status)}</Badge>
                            </td>
                            <td data-label="Статус" className="rtable-stack">
                                <Form.Select
                                    size="sm"
                                    value={draft.status}
                                    onChange={e => setDraft(order.id, { status: e.target.value })}
                                    disabled={savingId === order.id}
                                >
                                    {CUSTOM_ORDER_STATUSES.map(s => (
                                        <option key={s} value={s}>{customOrderStatusLabel(s)}</option>
                                    ))}
                                </Form.Select>
                            </td>
                            <td data-label="Цена, ₽" className="rtable-stack">
                                <Form.Control
                                    size="sm"
                                    type="number"
                                    min={0}
                                    step="0.01"
                                    placeholder="—"
                                    value={draft.price}
                                    onChange={e => setDraft(order.id, { price: e.target.value })}
                                    disabled={savingId === order.id}
                                />
                            </td>
                            <td className="rtable-actions">
                                <div className="d-flex gap-2">
                                    <Button
                                        size="sm"
                                        variant="success"
                                        onClick={() => handleSave(order)}
                                        disabled={savingId === order.id}
                                    >
                                        {savingId === order.id ? <Spinner animation="border" size="sm" /> : 'Сохранить'}
                                    </Button>
                                    <Button
                                        size="sm"
                                        variant="outline-secondary"
                                        onClick={() => setExpertOrder(order)}
                                        title="Консультация с экспертом по сборке"
                                    >
                                        🛠 Эксперт
                                    </Button>
                                </div>
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </Table>

        <ExpertChatModal
            show={expertOrder !== null}
            order={expertOrder}
            imagePreview={expertOrder?.imageUrl ? images[expertOrder.imageUrl] ?? null : null}
            onHide={() => setExpertOrder(null)}
        />
        </>
    );
};
