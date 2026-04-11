import React, { useState, useEffect } from 'react';
import { Form, Button, Row, Col, Spinner, Nav, Tab } from 'react-bootstrap';
import { adminService } from '../../services/adminService';
import { categoryService, Category } from '../../services/categoryService';
import { CreateGoodsRequest, UpdateGoodsRequest } from '../../types/admin';
import { toast } from 'react-toastify';
import type { Goods } from '../../types/goods';

interface GoodsFormProps {
    goods: Goods | null;
    onSuccess: () => void;
    onCancel: () => void;
}

type FormTab = 'main' | 'details' | 'care';

export const GoodsForm: React.FC<GoodsFormProps> = ({ goods, onSuccess, onCancel }) => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState<FormTab>('main');

    const [formData, setFormData] = useState({
        name: goods?.name || '',
        description: goods?.description || '',
        price: goods?.price || 0,
        categoryId: goods?.category?.id || '',
        difficulty: goods?.difficulty || 'Средне',
        duration: goods?.duration || undefined,
        videoUrl: goods?.videoUrl || '',
        detailedDescription: goods?.detailedDescription || '',
        careInstructions: goods?.careInstructions || ''
    });

    useEffect(() => {
        loadCategories();
    }, []);

    const loadCategories = async () => {
        try {
            const data = await categoryService.getAllCategories();
            setCategories(data);
        } catch (error) {
            toast.error('Ошибка загрузки категорий');
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'price' || name === 'duration' ? (value === '' ? undefined : Number(value)) : value
        }));
    };


    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            if (goods) {
                const updateData: UpdateGoodsRequest = { ...formData, id: goods.id };
                await adminService.updateGoods(goods.id, updateData);
                toast.success('Товар обновлен');
            } else {
                const createData: CreateGoodsRequest = formData;
                await adminService.createGoods(createData);
                toast.success('Товар создан');
            }
            onSuccess();
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Ошибка сохранения товара';
            toast.error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Form onSubmit={handleSubmit}>
            <Tab.Container activeKey={activeTab} onSelect={(k) => setActiveTab(k as FormTab)}>
                <Nav variant="tabs" className="mb-3">
                    <Nav.Item>
                        <Nav.Link eventKey="main">Основное</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="details">Детальное описание</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="care">Уход</Nav.Link>
                    </Nav.Item>
                </Nav>

                <Tab.Content>
                    <Tab.Pane eventKey="main">
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Название *</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="name"
                                        value={formData.name}
                                        onChange={handleChange}
                                        required
                                        minLength={3}
                                        maxLength={500}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Категория *</Form.Label>
                                    <Form.Select
                                        name="categoryId"
                                        value={formData.categoryId}
                                        onChange={handleChange}
                                        required
                                    >
                                        <option value="">Выберите категорию</option>
                                        {categories.map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                    </Form.Select>
                                </Form.Group>
                            </Col>
                        </Row>

                        <Form.Group className="mb-3">
                            <Form.Label>Описание *</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                name="description"
                                value={formData.description}
                                onChange={handleChange}
                                required
                                minLength={10}
                                maxLength={2000}
                            />
                        </Form.Group>

                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Цена (₽) *</Form.Label>
                                    <Form.Control
                                        type="number"
                                        name="price"
                                        value={formData.price}
                                        onChange={handleChange}
                                        min="0"
                                        step="0.01"
                                        required
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Уровень сложности</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="difficulty"
                                        value={formData.difficulty}
                                        onChange={handleChange}
                                        maxLength={100}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>

                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>URL видео (для курсов)</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="videoUrl"
                                        value={formData.videoUrl}
                                        onChange={handleChange}
                                        maxLength={500}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>

                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Длительность (минуты, для курсов)</Form.Label>
                                    <Form.Control
                                        type="number"
                                        name="duration"
                                        value={formData.duration || ''}
                                        onChange={handleChange}
                                        min="1"
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                    </Tab.Pane>

                    <Tab.Pane eventKey="details">
                        <Form.Group className="mb-3">
                            <Form.Label>Детальное описание (Markdown)</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={15}
                                name="detailedDescription"
                                value={formData.detailedDescription}
                                onChange={handleChange}
                                maxLength={10000}
                                placeholder="Используйте Markdown для форматирования текста"
                            />
                            <Form.Text className="text-muted">
                                Поддерживается Markdown синтаксис: **жирный**, *курсив*, заголовки (#), списки, ссылки и т.д.
                            </Form.Text>
                        </Form.Group>
                    </Tab.Pane>

                    <Tab.Pane eventKey="care">
                        <Form.Group className="mb-3">
                            <Form.Label>Рекомендации по уходу (Markdown)</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={15}
                                name="careInstructions"
                                value={formData.careInstructions}
                                onChange={handleChange}
                                maxLength={5000}
                                placeholder="Используйте Markdown для форматирования текста"
                            />
                            <Form.Text className="text-muted">
                                Поддерживается Markdown синтаксис: **жирный**, *курсив*, заголовки (#), списки, ссылки и т.д.
                            </Form.Text>
                        </Form.Group>
                    </Tab.Pane>
                </Tab.Content>
            </Tab.Container>

            <div className="d-flex justify-content-end gap-2 mt-3">
                <Button variant="secondary" onClick={onCancel} disabled={loading}>
                    Отмена
                </Button>
                <Button variant="success" type="submit" disabled={loading}>
                    {loading ? (
                        <>
                            <Spinner animation="border" size="sm" className="me-2" />
                            Сохранение...
                        </>
                    ) : (
                        'Сохранить'
                    )}
                </Button>
            </div>
        </Form>
    );
};
