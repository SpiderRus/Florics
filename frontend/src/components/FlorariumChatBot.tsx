import React, { useState, useEffect, useRef } from 'react';
import { Form, Button, Spinner, Alert } from 'react-bootstrap';
import { SendFill } from 'react-bootstrap-icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { aiBotService } from '../services/aiBotService';
import { Conversation, FlorariumMessage } from '../types/aibot';
import { useCart } from '../contexts/CartContext';
import MarkdownContent from './MarkdownContent';
import ImageModal from './ImageModal';
import CustomOrderModal from './CustomOrderModal';
import LoadingSpinner from './LoadingSpinner';
import { useChatScroll } from '../hooks/useChatScroll';

interface FlorariumChatBotProps {
    isAuthenticated: boolean;
    isBuyer: boolean;
}

/**
 * Чат-дизайнер флорариумов: пользователь описывает флорариум, бот стримит текст и генерирует
 * картинки. По картинке позже можно будет оформить заказ (кнопка-заглушка, заказ не реализован).
 *
 * Отличия от AiChatBot: сообщения несут картинки (blob objectURL), иной транспорт стрима
 * (события text/image), общий разговор без привязки к товару, освобождение blob-URL при размонтировании.
 */
const FlorariumChatBot: React.FC<FlorariumChatBotProps> = ({ isAuthenticated, isBuyer }) => {
    const location = useLocation();
    const navigate = useNavigate();
    const { addCustomFlorarium } = useCart();

    const [conversation, setConversation] = useState<Conversation | null>(null);
    const [messages, setMessages] = useState<FlorariumMessage[]>([]);
    const [inputMessage, setInputMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [error, setError] = useState<string | null>(null);
    // Индекс открытой в полноэкранном превью картинки (в общем списке картинок чата); null — закрыто
    const [previewIndex, setPreviewIndex] = useState<number | null>(null);
    // Оформление заказа по выбранной картинке
    const [orderOpen, setOrderOpen] = useState(false);
    const [orderSubmitting, setOrderSubmitting] = useState(false);
    // Выбранная для заказа картинка: blobUrl — для превью, sourceUrl — устойчивый прокси-URL для заказа
    const [orderImage, setOrderImage] = useState<{ blobUrl: string; sourceUrl: string } | null>(null);

    const inputRef = useRef<HTMLInputElement>(null);
    // Реестр созданных blob-URL картинок — освобождаем при размонтировании, чтобы не текла память
    const objectUrlsRef = useRef<string[]>([]);

    // Общий автоскролл (прилипание к низу); scrollIfSticky — доскролл после догрузки картинки
    const { containerRef: messagesContainerRef, handleScroll, scrollIfSticky } = useChatScroll(messages);

    // Создать или получить разговор дизайнера флорариумов
    useEffect(() => {
        if (!isAuthenticated || !isBuyer) {
            setLoading(false);
            return;
        }

        const initConversation = async () => {
            try {
                setLoading(true);
                setError(null);
                const conv = await aiBotService.createOrGetFlorariumConversation();
                setConversation(conv);

                // Восстановить историю диалога (текст + картинки) при перезагрузке страницы
                const history = await aiBotService.getMessages(conv.id);
                const restored: FlorariumMessage[] = history
                    .filter(m => m.content.trim().length > 0 || (m.imageUrls?.length ?? 0) > 0)
                    .map(m => ({
                        id: m.id,
                        role: m.role.toLowerCase() === 'user' ? 'user' : 'assistant',
                        content: m.content,
                        images: [],
                        imageSources: [],
                        pendingImages: m.imageUrls?.length ?? 0,
                        createdAt: m.createdAt
                    } as FlorariumMessage));
                setMessages(restored);

                // Догрузить картинки истории как blob и подставить в соответствующие сообщения
                history.forEach(m => {
                    (m.imageUrls ?? []).forEach(url => {
                        aiBotService.fetchFlorariumImage(url)
                            .then(objectUrl => {
                                objectUrlsRef.current.push(objectUrl);
                                setMessages(prev => prev.map(x =>
                                    x.id === m.id
                                        ? {
                                            ...x,
                                            images: [...x.images, objectUrl],
                                            imageSources: [...x.imageSources, url],
                                            pendingImages: Math.max(0, x.pendingImages - 1)
                                        }
                                        : x
                                ));
                            })
                            .catch(err => {
                                setMessages(prev => prev.map(x =>
                                    x.id === m.id ? { ...x, pendingImages: Math.max(0, x.pendingImages - 1) } : x
                                ));
                                if (err?.message !== 'Unauthorized') {
                                    console.error('Failed to load florarium history image:', err);
                                }
                            });
                    });
                });
            } catch (err: any) {
                console.error('Failed to initialize florarium conversation:', err);
                setError('Дизайнер флорариумов сейчас не на связи. Пожалуйста, загляните чуть позже.');
            } finally {
                setLoading(false);
            }
        };

        initConversation();
    }, [isAuthenticated, isBuyer]);

    // Фокус на поле ввода после инициализации
    useEffect(() => {
        if (!loading && conversation && inputRef.current) {
            inputRef.current.focus();
        }
    }, [loading, conversation]);

    // Освободить blob-URL картинок при размонтировании
    useEffect(() => {
        return () => {
            objectUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            objectUrlsRef.current = [];
        };
    }, []);

    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!inputMessage.trim() || !conversation || sending) return;

        const userMessage = inputMessage.trim();
        setInputMessage('');
        setSending(true);
        setError(null);

        const tempUserMessage: FlorariumMessage = {
            id: 'temp-user-' + Date.now(),
            role: 'user',
            content: userMessage,
            images: [],
            imageSources: [],
            pendingImages: 0,
            createdAt: new Date().toISOString()
        };
        const botMessageId = 'temp-bot-' + Date.now();
        setMessages(prev => [...prev, tempUserMessage]);

        let botInserted = false;
        // Вставить (при первом контенте) и обновить сообщение бота
        const upsertBot = (mutate: (m: FlorariumMessage) => FlorariumMessage) => {
            setMessages(prev => {
                let list = prev;
                if (!botInserted) {
                    botInserted = true;
                    list = [...prev, {
                        id: botMessageId,
                        role: 'assistant',
                        content: '',
                        images: [],
                        imageSources: [],
                        pendingImages: 0,
                        createdAt: new Date().toISOString()
                    }];
                }
                return list.map(m => (m.id === botMessageId ? mutate(m) : m));
            });
        };

        try {
            await aiBotService.sendFlorariumMessageStream(conversation.id, userMessage, (event) => {
                if (event.type === 'text' && event.text) {
                    const text = event.text;
                    upsertBot(m => ({ ...m, content: m.content + text }));
                } else if (event.type === 'image' && event.imageUrl) {
                    // Картинка приходит ссылкой — показываем спиннер-плейсхолдер на время загрузки blob,
                    // затем заменяем его картинкой
                    const sourceUrl = event.imageUrl;
                    upsertBot(m => ({ ...m, pendingImages: m.pendingImages + 1 }));
                    aiBotService.fetchFlorariumImage(sourceUrl)
                        .then(objectUrl => {
                            objectUrlsRef.current.push(objectUrl);
                            upsertBot(m => ({
                                ...m,
                                images: [...m.images, objectUrl],
                                imageSources: [...m.imageSources, sourceUrl],
                                pendingImages: Math.max(0, m.pendingImages - 1)
                            }));
                        })
                        .catch(err => {
                            upsertBot(m => ({ ...m, pendingImages: Math.max(0, m.pendingImages - 1) }));
                            if (err?.message !== 'Unauthorized') {
                                console.error('Failed to load florarium image:', err);
                            }
                        });
                }
            });
        } catch (err: any) {
            console.error('Failed to send florarium message:', err);
            setMessages(prev => prev.filter(m => m.id !== tempUserMessage.id && m.id !== botMessageId));

            if (err.status === 503) {
                setError('Дизайнер флорариумов временно недоступен. Попробуйте позже.');
            } else if (err.message !== 'Unauthorized') {
                setError('Не удалось отправить сообщение. Попробуйте еще раз.');
            }
        } finally {
            setSending(false);
            setTimeout(() => inputRef.current?.focus(), 100);
        }
    };

    // «Закончить разговор» — создать новый разговор, очистить экран и освободить blob-картинки
    const handleEndConversation = async () => {
        if (sending || loading) return;
        try {
            setLoading(true);
            setError(null);
            const newConv = await aiBotService.createNewFlorariumConversation();
            setPreviewIndex(null);
            objectUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            objectUrlsRef.current = [];
            setConversation(newConv);
            setMessages([]);
            setInputMessage('');
        } catch (err: any) {
            console.error('Failed to start new florarium conversation:', err);
            if (err?.message !== 'Unauthorized') {
                setError('Не удалось начать новый разговор. Попробуйте ещё раз.');
            }
        } finally {
            setLoading(false);
            setTimeout(() => inputRef.current?.focus(), 100);
        }
    };

    // Открыть модалку заказа по выбранной картинке
    const openOrder = (blobUrl: string, sourceUrl: string | undefined) => {
        if (!sourceUrl) {
            toast.error('Не удалось определить картинку для заказа');
            return;
        }
        setOrderImage({ blobUrl, sourceUrl });
        setOrderOpen(true);
    };

    // Добавить выбранный флорариум в корзину
    const handleOrderSubmit = async (comment: string, contact: string) => {
        if (!conversation || !orderImage) return;
        setOrderSubmitting(true);
        try {
            await addCustomFlorarium({
                conversationId: conversation.id,
                imageUrl: orderImage.sourceUrl,
                comment: comment || undefined,
                contact: contact || undefined
            });
            setOrderOpen(false);
            setOrderImage(null);
            toast.success(
                <span>
                    Добавлено в корзину!{' '}
                    <span
                        style={{ textDecoration: 'underline', cursor: 'pointer' }}
                        onClick={() => navigate('/cart')}
                    >
                        Перейти в корзину
                    </span>
                </span>
            );
        } catch (err: any) {
            if (err?.message !== 'Unauthorized') {
                toast.error('Не удалось добавить в корзину. Попробуйте ещё раз.');
            }
        } finally {
            setOrderSubmitting(false);
        }
    };

    // Сценарий 1: не авторизован
    if (!isAuthenticated) {
        return (
            <div className="chat-container florarium-chat">
                <div className="chat-empty-state" style={{ height: '100%', justifyContent: 'center' }}>
                    <h5>🪴 Дизайнер флорариума</h5>
                    <p>Чтобы создать свой флорариум с помощью AI, необходимо войти в аккаунт.</p>
                    <div className="mt-3">
                        <Button
                            variant="success"
                            onClick={() => navigate('/login', { state: { from: location.pathname } })}
                            className="me-2"
                        >
                            Войти
                        </Button>
                        <Button
                            variant="outline-success"
                            onClick={() => navigate('/register', { state: { from: location.pathname } })}
                        >
                            Зарегистрироваться
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    // Сценарий 2: авторизован, но не покупатель
    if (!isBuyer) {
        return (
            <div className="chat-container florarium-chat">
                <div className="chat-empty-state" style={{ height: '100%', justifyContent: 'center' }}>
                    <h5>🪴 Дизайнер флорариума</h5>
                    <p>К сожалению, создание флорариума с AI доступно только пользователям с правами покупателя.</p>
                    <p className="text-muted mt-2">
                        Если вы считаете, что это ошибка, пожалуйста, свяжитесь с администратором.
                    </p>
                </div>
            </div>
        );
    }

    // Сценарий 3: загрузка
    if (loading && !conversation) {
        return <LoadingSpinner text="Загрузка дизайнера..." />;
    }

    // Все картинки чата одним плоским списком — для навигации в полноэкранной карусели
    const allImages = messages.flatMap(m => m.images);

    return (
        <div className="chat-container florarium-chat">
            <div className="chat-header">
                <h5 className="mb-0">🪴 Дизайнер флорариума</h5>
                <Button
                    variant="outline-light"
                    size="sm"
                    className="florarium-end-btn"
                    onClick={handleEndConversation}
                    disabled={sending || loading || messages.length === 0}
                >
                    Начать новый
                </Button>
            </div>

            {error && (
                <Alert variant="danger" dismissible onClose={() => setError(null)} className="mx-3 mt-3">
                    {error}
                </Alert>
            )}

            <div
                className="chat-messages"
                ref={messagesContainerRef}
                onScroll={handleScroll}
                role="log"
                aria-live="polite"
                aria-relevant="additions text"
                aria-label="Диалог с дизайнером флорариумов"
            >
                {messages.length === 0 ? (
                    <div className="chat-empty-state">
                        <h5>🪴 Дизайнер флорариума</h5>
                        <p>Опишите флорариум вашей мечты — растения, размер и форму контейнера, стиль.</p>
                        <p>AI подберёт решение и нарисует, как он может выглядеть!</p>
                    </div>
                ) : (
                    messages.map((message) => (
                        <div
                            key={message.id}
                            className={`chat-message ${message.role === 'user' ? 'chat-message-user' : 'chat-message-bot'}${
                                message.images.length > 0 || message.pendingImages > 0 ? ' has-image' : ''
                            }`}
                        >
                            {message.role === 'assistant' ? (
                                <>
                                    {message.content && <MarkdownContent content={message.content} />}
                                    {message.images.map((src, idx) => (
                                        <div key={idx} className="florarium-image-block">
                                            <img
                                                className="chat-message-image"
                                                src={src}
                                                alt="Сгенерированный флорариум"
                                                onLoad={scrollIfSticky}
                                                onClick={() => setPreviewIndex(allImages.indexOf(src))}
                                            />
                                            <Button
                                                variant="outline-success"
                                                size="sm"
                                                className="florarium-order-btn"
                                                onClick={() => openOrder(src, message.imageSources[idx])}
                                                disabled={orderSubmitting}
                                                title="Оформить заказ по этому дизайну"
                                            >
                                                Заказать этот флорариум
                                            </Button>
                                        </div>
                                    ))}
                                    {/* Плейсхолдеры картинок, которые ещё догружаются */}
                                    {Array.from({ length: message.pendingImages }).map((_, idx) => (
                                        <div key={`pending-${idx}`} className="florarium-image-placeholder">
                                            <Spinner animation="border" variant="success" size="sm" />
                                            <span>Рисую флорариум…</span>
                                        </div>
                                    ))}
                                </>
                            ) : (
                                <div>{message.content}</div>
                            )}
                        </div>
                    ))
                )}

                {/* Индикатор «генерирует…» — держится, пока бот не вернёт ответ целиком */}
                {sending && (
                    <div className="chat-message chat-message-bot">
                        <div className="chat-typing-indicator" role="status" aria-label="Дизайнер рисует">
                            <span aria-hidden="true"></span>
                            <span aria-hidden="true"></span>
                            <span aria-hidden="true"></span>
                        </div>
                    </div>
                )}
            </div>

            <div className="chat-input-box">
                <Form onSubmit={handleSendMessage} className="d-flex w-100">
                    <Form.Control
                        ref={inputRef}
                        type="text"
                        placeholder="Опишите флорариум вашей мечты..."
                        value={inputMessage}
                        onChange={(e) => setInputMessage(e.target.value)}
                        disabled={sending || loading || !conversation}
                        className="chat-input"
                    />
                    <Button
                        type="submit"
                        variant="success"
                        disabled={!inputMessage.trim() || sending || loading || !conversation}
                        className="ms-2 chat-send-button"
                        aria-label="Отправить сообщение"
                    >
                        {sending ? <Spinner animation="border" size="sm" /> : <SendFill aria-hidden="true" />}
                    </Button>
                </Form>
            </div>

            {/* Полноэкранный просмотр картинок чата с навигацией, открывается на выбранной */}
            {previewIndex !== null && allImages.length > 0 && (
                <ImageModal
                    show={previewIndex !== null}
                    image={allImages[previewIndex]}
                    images={allImages}
                    currentIndex={previewIndex}
                    onHide={() => setPreviewIndex(null)}
                    onNavigate={setPreviewIndex}
                />
            )}

            {/* Модалка оформления заказа по выбранной картинке */}
            <CustomOrderModal
                show={orderOpen}
                imagePreview={orderImage?.blobUrl ?? null}
                submitting={orderSubmitting}
                onSubmit={handleOrderSubmit}
                onHide={() => {
                    if (!orderSubmitting) {
                        setOrderOpen(false);
                        setOrderImage(null);
                    }
                }}
            />
        </div>
    );
};

export default FlorariumChatBot;
