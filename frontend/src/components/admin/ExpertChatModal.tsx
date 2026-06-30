import React, { useEffect, useRef, useState } from 'react';
import { Modal, Form, Button, Spinner, Alert } from 'react-bootstrap';
import { SendFill } from 'react-bootstrap-icons';
import { toast } from 'react-toastify';
import { customOrderService } from '../../services/customOrderService';
import { aiBotService } from '../../services/aiBotService';
import { CustomOrder, ExpertMessage } from '../../types/customOrder';
import MarkdownContent from '../MarkdownContent';
import ImageModal from '../ImageModal';

interface ExpertChatModalProps {
    show: boolean;
    order: CustomOrder | null;
    /** blob-objectURL превью дизайна (если уже загружен в таблице) — мгновенный показ до загрузки карусели */
    imagePreview?: string | null;
    onHide: () => void;
}

/**
 * Чат мастера с экспертом-ассистентом (florarium-expert) по конкретному заказу.
 * Стиль чата — как в остальных чатах (классы chat-*). Текст-только: эксперт видит дизайн заказа
 * и историю разговора-дизайнера (на стороне бота). По клику на превью открывается карусель всех
 * картинок исходного чата пользователя (активная — выбранная в заказе).
 */
const ExpertChatModal: React.FC<ExpertChatModalProps> = ({ show, order, imagePreview, onHide }) => {
    const [messages, setMessages] = useState<ExpertMessage[]>([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [sending, setSending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Картинки дизайна (blob objectURL, выровнены по индексу с designImages); selectedIndex — заказанная
    const [designBlobs, setDesignBlobs] = useState<string[]>([]);
    const [selectedIndex, setSelectedIndex] = useState(0);
    // Индекс открытой в полноэкранной карусели картинки (в gallery); null — закрыто
    const [carouselIndex, setCarouselIndex] = useState<number | null>(null);

    const messagesRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const objectUrls = useRef<string[]>([]);

    const scrollToBottom = () => {
        const c = messagesRef.current;
        if (c) c.scrollTo({ top: c.scrollHeight, behavior: 'smooth' });
    };

    useEffect(() => {
        if (messages.length > 0) scrollToBottom();
    }, [messages]);

    // Освобождение blob-URL карусели при размонтировании
    useEffect(() => () => {
        objectUrls.current.forEach(u => URL.revokeObjectURL(u));
        objectUrls.current = [];
    }, []);

    // Загрузка истории и картинок дизайна при открытии
    useEffect(() => {
        if (!show || !order) return;

        let cancelled = false;
        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                setMessages([]);
                setDesignBlobs([]);
                setSelectedIndex(0);
                setCarouselIndex(null);

                const session = await customOrderService.openExpertChat(order.id);
                if (cancelled) return;

                const restored: ExpertMessage[] = session.messages
                    .filter(m => m.content.trim().length > 0)
                    .map(m => ({
                        id: m.id,
                        role: m.role.toUpperCase() === 'USER' ? 'user' : 'assistant',
                        content: m.content
                    }));
                setMessages(restored);

                // Картинки дизайна: грузим как blob (по порядку), активная — выбранная в заказе
                const urls = session.designImages ?? [];
                const sel = session.selectedImageUrl ? urls.indexOf(session.selectedImageUrl) : 0;
                setSelectedIndex(sel >= 0 ? sel : 0);

                const blobs: string[] = new Array(urls.length).fill('');
                await Promise.all(urls.map((u, i) =>
                    aiBotService.fetchFlorariumImage(u)
                        .then(b => { objectUrls.current.push(b); blobs[i] = b; })
                        .catch(() => { /* пропускаем недоступную картинку */ })
                ));
                if (!cancelled) setDesignBlobs(blobs);
            } catch (err: any) {
                if (cancelled) return;
                console.error('Failed to open expert chat:', err);
                setError('Не удалось открыть чат с экспертом. Убедитесь, что у заказа есть дизайн, и попробуйте снова.');
            } finally {
                if (!cancelled) {
                    setLoading(false);
                    setTimeout(() => inputRef.current?.focus(), 100);
                }
            }
        };
        load();
        return () => { cancelled = true; };
    }, [show, order]);

    const handleSend = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!input.trim() || !order || sending) return;

        const text = input.trim();
        setInput('');
        setSending(true);
        setError(null);

        const userMsg: ExpertMessage = { id: 'u-' + Date.now(), role: 'user', content: text };
        const botId = 'a-' + Date.now();
        setMessages(prev => [...prev, userMsg]);

        let botInserted = false;
        const appendToBot = (token: string) => {
            setMessages(prev => {
                if (!botInserted) {
                    botInserted = true;
                    return [...prev, { id: botId, role: 'assistant', content: token }];
                }
                return prev.map(m => (m.id === botId ? { ...m, content: m.content + token } : m));
            });
        };

        try {
            await customOrderService.streamExpertMessage(order.id, text, appendToBot);
        } catch (err: any) {
            console.error('Failed to stream expert message:', err);
            setMessages(prev => prev.filter(m => m.id !== botId));
            if (err?.message !== 'Unauthorized') {
                toast.error('Не удалось получить ответ эксперта. Попробуйте ещё раз.');
            }
        } finally {
            setSending(false);
            setTimeout(() => inputRef.current?.focus(), 100);
        }
    };

    // Только успешно загруженные картинки — для карусели
    const gallery = designBlobs.filter(Boolean);
    const previewSrc = designBlobs[selectedIndex] || imagePreview || null;

    const openCarousel = () => {
        if (gallery.length === 0) return;
        const sel = designBlobs[selectedIndex];
        const gi = sel ? gallery.indexOf(sel) : 0;
        setCarouselIndex(gi >= 0 ? gi : 0);
    };

    return (
        <Modal show={show} onHide={onHide} size="xl" centered>
            <Modal.Header closeButton>
                <Modal.Title>🛠 Консультация мастера</Modal.Title>
            </Modal.Header>
            <Modal.Body style={{ padding: 0 }}>
                <div className="chat-container" style={{ height: '72vh' }}>
                    {/* Шапка: дизайн заказа (кликабельно → карусель) + пожелания клиента */}
                    <div className="chat-header" style={{ alignItems: 'center', gap: '0.75rem' }}>
                        {previewSrc ? (
                            <img
                                src={previewSrc}
                                alt="Дизайн заказа"
                                onClick={openCarousel}
                                title="Открыть все картинки из чата"
                                style={{ width: 56, height: 56, objectFit: 'cover', borderRadius: 8, cursor: gallery.length ? 'pointer' : 'default', flexShrink: 0 }}
                            />
                        ) : (
                            <div style={{ width: 56, height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.2)', borderRadius: 8, fontSize: '1.6rem', flexShrink: 0 }}>🪴</div>
                        )}
                        <div style={{ minWidth: 0 }}>
                            <h5 className="mb-0">Кастомный флорариум</h5>
                            <div style={{ fontSize: '0.8rem', opacity: 0.85, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                {order?.customerComment ? `Пожелания: ${order.customerComment}` : 'Пожелания не указаны'}
                            </div>
                        </div>
                    </div>

                    {error && (
                        <Alert variant="danger" dismissible onClose={() => setError(null)} className="mx-3 mt-3">
                            {error}
                        </Alert>
                    )}

                    {/* Список сообщений */}
                    <div className="chat-messages" ref={messagesRef}>
                        {loading ? (
                            <div className="text-center py-5">
                                <Spinner animation="border" variant="success" />
                                <p className="mt-2">Загрузка...</p>
                            </div>
                        ) : messages.length === 0 ? (
                            <div className="chat-empty-state">
                                <h5>🛠 Эксперт по сборке</h5>
                                <p>Спросите, как собрать этот флорариум: материалы, слои дренажа, посадка, уход.</p>
                            </div>
                        ) : (
                            messages.map(message => (
                                <div
                                    key={message.id}
                                    className={`chat-message ${message.role === 'user' ? 'chat-message-user' : 'chat-message-bot'}`}
                                >
                                    {message.role === 'assistant'
                                        ? <MarkdownContent content={message.content} />
                                        : <div>{message.content}</div>}
                                </div>
                            ))
                        )}

                        {/* Индикатор «печатает…» — держится, пока эксперт не вернёт ответ целиком */}
                        {sending && (
                            <div className="chat-message chat-message-bot">
                                <div className="chat-typing-indicator">
                                    <span></span>
                                    <span></span>
                                    <span></span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Поле ввода */}
                    <div className="chat-input-box">
                        <Form onSubmit={handleSend} className="d-flex w-100">
                            <Form.Control
                                ref={inputRef}
                                type="text"
                                placeholder="Спросите эксперта о сборке флорариума..."
                                value={input}
                                onChange={e => setInput(e.target.value)}
                                disabled={sending || loading || !order}
                                className="chat-input"
                            />
                            <Button
                                type="submit"
                                variant="success"
                                disabled={!input.trim() || sending || loading || !order}
                                className="ms-2 chat-send-button"
                            >
                                {sending ? <Spinner animation="border" size="sm" /> : <SendFill aria-hidden="true" />}
                            </Button>
                        </Form>
                    </div>
                </div>
            </Modal.Body>

            {/* Полноэкранная карусель картинок из чата пользователя (активная — заказанная) */}
            {carouselIndex !== null && gallery.length > 0 && (
                <ImageModal
                    show={carouselIndex !== null}
                    image={gallery[carouselIndex]}
                    images={gallery}
                    currentIndex={carouselIndex}
                    onHide={() => setCarouselIndex(null)}
                    onNavigate={setCarouselIndex}
                />
            )}
        </Modal>
    );
};

export default ExpertChatModal;
