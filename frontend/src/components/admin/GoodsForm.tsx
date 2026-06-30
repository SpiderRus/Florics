import React, { useState, useEffect } from 'react';
import { Form, Button, Row, Col, Spinner, Nav, Tab } from 'react-bootstrap';
import { adminService } from '../../services/adminService';
import { categoryService, Category } from '../../services/categoryService';
import { CreateGoodsRequest, UpdateGoodsRequest } from '../../types/admin';
import { toast } from 'react-toastify';
import type { Goods } from '../../types/goods';
import { MediaManager, PhotoDraft } from './MediaManager';
import type { MediaReconcileItem } from '../../types/admin';

interface GoodsFormProps {
    goods: Goods | null;
    onSuccess: () => void;
    onCancel: () => void;
}

type FormTab = 'main' | 'details' | 'care' | 'photos';

export const GoodsForm: React.FC<GoodsFormProps> = ({ goods, onSuccess, onCancel }) => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState<FormTab>('main');
    const [photos, setPhotos] = useState<PhotoDraft[]>([]);
    const [mediaLoading, setMediaLoading] = useState(false);
    const [analyzing, setAnalyzing] = useState(false);
    const [analyzingName, setAnalyzingName] = useState(false);

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

    // Загрузка существующих фото при редактировании
    useEffect(() => {
        if (!goods?.id) return;
        let cancelled = false;
        setMediaLoading(true);
        adminService.getGoodsMedia(goods.id)
            .then(list => {
                if (cancelled) return;
                const drafts: PhotoDraft[] = list
                    .slice()
                    .sort((a, b) => a.order - b.order)
                    .map(m => ({ key: m.id, mediaId: m.id, url: m.url, previewUrl: m.url }));
                setPhotos(drafts);
            })
            .catch(() => { if (!cancelled) toast.error('Не удалось загрузить фотографии товара'); })
            .finally(() => { if (!cancelled) setMediaLoading(false); });
        return () => { cancelled = true; };
    }, [goods?.id]);

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
            // 1) Сохранить скалярные поля товара, получить id
            let saved: Goods;
            if (goods) {
                const updateData: UpdateGoodsRequest = { ...formData, id: goods.id };
                saved = await adminService.updateGoods(goods.id, updateData);
            } else {
                const createData: CreateGoodsRequest = formData;
                saved = await adminService.createGoods(createData);
            }
            const goodsId = saved.id!;

            // 2) Догрузить новые файлы, собрать финальный упорядоченный список
            const items: MediaReconcileItem[] = [];
            for (const p of photos) {
                if (p.file) {
                    const created = await adminService.uploadGoodsPhoto(goodsId, p.file);
                    items.push({ id: created.id, order: items.length });
                } else if (p.mediaId) {
                    items.push({ id: p.mediaId, order: items.length });
                } else if (p.url) {
                    items.push({ url: p.url, order: items.length });
                }
            }

            // 3) Привести набор медиа к финальному списку (удаления/вставки/порядок)
            await adminService.reconcileGoodsMedia(goodsId, items);

            toast.success(goods ? 'Товар обновлён' : 'Товар создан');
            onSuccess();
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Ошибка сохранения товара';
            toast.error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    // Анализ фотографий ботом → перезапись полей карточки
    const handleAnalyze = async () => {
        setAnalyzing(true);
        try {
            const card = await adminService.analyzePhotos(photos);
            if (card.error) {
                toast.error(card.error);
                return;
            }
            setFormData(prev => ({
                ...prev,
                name: card.name ?? prev.name,
                description: card.shortDescription ?? prev.description,
                detailedDescription: card.fullDescription ?? prev.detailedDescription,
                careInstructions: card.care ?? prev.careInstructions,
                categoryId: categories.find(c => c.type === 'PLANT')?.id ?? prev.categoryId
            }));
            toast.success('Поля заполнены по фото');
        } catch {
            toast.error('Не удалось проанализировать фото');
        } finally {
            setAnalyzing(false);
        }
    };

    // Заполнение полей карточки по введённому названию (без передачи фото). Название не перезаписываем.
    const handleAnalyzeByName = async () => {
        const name = formData.name.trim();
        if (!name) return;
        setAnalyzingName(true);
        try {
            const card = await adminService.analyzeByName(name);
            if (card.error) {
                toast.error(card.error);
                return;
            }
            setFormData(prev => ({
                ...prev,
                description: card.shortDescription ?? prev.description,
                detailedDescription: card.fullDescription ?? prev.detailedDescription,
                careInstructions: card.care ?? prev.careInstructions,
                categoryId: categories.find(c => c.type === 'PLANT')?.id ?? prev.categoryId
            }));
            toast.success('Поля заполнены по названию');
        } catch {
            toast.error('Не удалось проанализировать название');
        } finally {
            setAnalyzingName(false);
        }
    };

    return (
        <Form onSubmit={handleSubmit}>
            <Tab.Container activeKey={activeTab} onSelect={(k) => setActiveTab(k as FormTab)}>
                <Nav variant="tabs" className="admin-tabs mb-3">
                    <Nav.Item>
                        <Nav.Link eventKey="main">Основное</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="details">Детальное описание</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="care">Уход</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="photos">Фотографии</Nav.Link>
                    </Nav.Item>
                </Nav>

                <Tab.Content>
                    <Tab.Pane eventKey="main">
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <div className="d-flex align-items-center justify-content-between gap-2">
                                        <Form.Label className="mb-0">Название *</Form.Label>
                                        <Button
                                            variant="outline-success"
                                            size="sm"
                                            disabled={!formData.name.trim() || analyzingName || loading}
                                            onClick={handleAnalyzeByName}
                                            title="Заполнить описание, детали, уход и категорию по названию"
                                        >
                                            {analyzingName
                                                ? <><Spinner animation="border" size="sm" className="me-1" />Анализ…</>
                                                : '✨ Заполнить по названию'}
                                        </Button>
                                    </div>
                                    <Form.Control
                                        type="text"
                                        name="name"
                                        value={formData.name}
                                        onChange={handleChange}
                                        required
                                        minLength={3}
                                        maxLength={500}
                                        className="mt-1"
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

                    <Tab.Pane eventKey="photos">
                        {mediaLoading
                            ? <div className="text-muted py-4 text-center">Загрузка фотографий…</div>
                            : <MediaManager photos={photos} onChange={setPhotos} onAnalyze={handleAnalyze} analyzing={analyzing} />}
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
