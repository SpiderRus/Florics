import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Spinner } from 'react-bootstrap';

interface CustomOrderModalProps {
    show: boolean;
    /** blob-objectURL выбранной картинки для превью */
    imagePreview: string | null;
    submitting: boolean;
    onSubmit: (comment: string, contact: string) => void;
    onHide: () => void;
}

/**
 * Модалка оформления заказа кастомного флорариума по выбранной картинке:
 * комментарий (пожелания) и контакты (опционально). Добавляет заказ в корзину.
 */
const CustomOrderModal: React.FC<CustomOrderModalProps> = ({ show, imagePreview, submitting, onSubmit, onHide }) => {
    const [comment, setComment] = useState('');
    const [contact, setContact] = useState('');

    // Сбрасываем поля при каждом открытии (новый выбор картинки)
    useEffect(() => {
        if (show) {
            setComment('');
            setContact('');
        }
    }, [show]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit(comment.trim(), contact.trim());
    };

    return (
        <Modal show={show} onHide={onHide} centered>
            <Modal.Header closeButton>
                <Modal.Title>Заказать флорариум</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {imagePreview && (
                        <div className="text-center mb-3">
                            <img
                                src={imagePreview}
                                alt="Выбранный флорариум"
                                style={{ maxWidth: '100%', maxHeight: 240, borderRadius: 8 }}
                            />
                        </div>
                    )}
                    <p className="text-muted small">
                        Мы добавим этот дизайн в корзину. После оформления заказа менеджер свяжется с вами,
                        согласует детали и цену.
                    </p>
                    <Form.Group className="mb-3" controlId="customOrderComment">
                        <Form.Label>Комментарий к заказу</Form.Label>
                        <Form.Control
                            as="textarea"
                            rows={3}
                            placeholder="Пожелания: размеры, растения, сроки..."
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            disabled={submitting}
                        />
                    </Form.Group>
                    <Form.Group className="mb-2" controlId="customOrderContact">
                        <Form.Label>Контакты для связи <span className="text-muted">(необязательно)</span></Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="Телефон, Telegram или e-mail"
                            value={contact}
                            onChange={(e) => setContact(e.target.value)}
                            disabled={submitting}
                        />
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="outline-secondary" onClick={onHide} disabled={submitting}>
                        Отмена
                    </Button>
                    <Button type="submit" variant="success" disabled={submitting}>
                        {submitting ? <Spinner animation="border" size="sm" /> : 'Добавить в корзину'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

export default CustomOrderModal;
