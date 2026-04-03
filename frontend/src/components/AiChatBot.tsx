import React, { useState, useEffect, useRef } from 'react';
import { Form, Button, Spinner, Alert, Modal } from 'react-bootstrap';
import { useLocation, useNavigate } from 'react-router-dom';
import { aiBotService } from '../services/aiBotService';
import { Conversation, Message } from '../types/aibot';
import MarkdownContent from './MarkdownContent';

interface AiChatBotProps {
    goodsId: number;
    goodsName: string;
    isAuthenticated: boolean;
    canPurchase: boolean;
}

const AiChatBot: React.FC<AiChatBotProps> = ({ goodsId, goodsName, isAuthenticated, canPurchase }) => {
    const location = useLocation();
    const navigate = useNavigate();

    const [conversation, setConversation] = useState<Conversation | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [inputMessage, setInputMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [showClearModal, setShowClearModal] = useState(false);
    const [isInitialLoad, setIsInitialLoad] = useState(true);

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const messagesContainerRef = useRef<HTMLDivElement>(null);
    const chatContainerRef = useRef<HTMLDivElement>(null);

    // Автоматическая прокрутка вниз при новых сообщениях
    const scrollToBottom = (instant: boolean = false) => {
        if (messagesContainerRef.current) {
            const container = messagesContainerRef.current;
            const scrollOptions: ScrollToOptions = {
                top: container.scrollHeight,
                behavior: instant ? 'auto' : 'smooth'
            };
            container.scrollTo(scrollOptions);
        }
    };

    useEffect(() => {
        if (messages.length > 0) {
            scrollToBottom(false);
        }
    }, [messages]);

    // Дополнительный скролл при завершении начальной загрузки
    useEffect(() => {
        if (!isInitialLoad && messages.length > 0) {
            // Даем время на рендеринг всех сообщений
            setTimeout(() => {
                scrollToBottom(true);
            }, 150);
        }
    }, [isInitialLoad]);

    // Скролл при первом рендере после загрузки
    useEffect(() => {
        if (!loading && messages.length > 0 && messagesContainerRef.current) {
            setTimeout(() => {
                scrollToBottom(true);
            }, 100);
        }
    }, [loading]);

    // Отслеживание видимости чата (когда вкладка активируется)
    useEffect(() => {
        const container = chatContainerRef.current;
        if (!container) return;

        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    // Когда чат становится видимым
                    if (entry.isIntersecting && messages.length > 0) {
                        setTimeout(() => {
                            scrollToBottom(true);
                        }, 100);
                    }
                });
            },
            { threshold: 0.1 }
        );

        observer.observe(container);

        return () => {
            observer.disconnect();
        };
    }, [messages]);

    // Загрузка или создание conversation при монтировании
    useEffect(() => {
        // Инициализация только для авторизованных покупателей
        if (!isAuthenticated || !canPurchase) {
            setLoading(false);
            return;
        }

        const initConversation = async () => {
            try {
                setLoading(true);
                setError(null);

                // Создать или получить существующий conversation
                const conv = await aiBotService.createOrGetConversation(goodsId, goodsName);
                setConversation(conv);

                // Загрузить историю сообщений
                const msgs = await aiBotService.getMessages(conv.id);
                setMessages(msgs);

                // Отметить что начальная загрузка завершена
                setIsInitialLoad(false);

                // Прокрутить вниз после загрузки
                setTimeout(() => {
                    scrollToBottom(true);
                }, 200);
            } catch (err: any) {
                console.error('Failed to initialize conversation:', err);
                if (err.response?.status === 503) {
                    setError('AI консультант временно недоступен. Попробуйте позже.');
                } else {
                    setError('Не удалось загрузить чат. Попробуйте обновить страницу.');
                }
            } finally {
                setLoading(false);
            }
        };

        initConversation();
    }, [goodsId, goodsName, isAuthenticated, canPurchase]);

    // Отправка сообщения
    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!inputMessage.trim() || !conversation || sending) return;

        const userMessage = inputMessage.trim();
        setInputMessage('');
        setSending(true);
        setError(null);

        // Оптимистично добавить сообщение пользователя в UI
        const tempUserMessage: Message = {
            id: 'temp-user-' + Date.now(),
            conversationId: conversation.id,
            role: 'user',
            content: userMessage,
            createdAt: new Date().toISOString()
        };
        setMessages(prev => [...prev, tempUserMessage]);

        try {
            // Отправить сообщение и получить ответ от AI
            const response = await aiBotService.sendMessage(conversation.id, userMessage);

            // Добавить ответ бота в UI
            const botMessage: Message = {
                id: response.messageId,
                conversationId: response.conversationId,
                role: 'assistant',
                content: response.response,
                createdAt: response.timestamp
            };

            // Обновить сообщение пользователя с реальным ID (если нужно) и добавить ответ бота
            setMessages(prev => [...prev, botMessage]);
        } catch (err: any) {
            console.error('Failed to send message:', err);
            // Удалить оптимистичное сообщение пользователя при ошибке
            setMessages(prev => prev.filter(m => m.id !== tempUserMessage.id));

            if (err.response?.status === 503) {
                setError('AI консультант временно недоступен. Попробуйте позже.');
            } else {
                setError('Не удалось отправить сообщение. Попробуйте еще раз.');
            }
        } finally {
            setSending(false);
        }
    };

    // Очистка истории
    const handleClearHistory = async () => {
        if (!conversation) return;

        try {
            setLoading(true);
            await aiBotService.deleteConversation(conversation.id);

            // Создать новый conversation
            const newConv = await aiBotService.createOrGetConversation(goodsId, goodsName);
            setConversation(newConv);
            setMessages([]);
            setShowClearModal(false);
        } catch (err: any) {
            console.error('Failed to clear history:', err);
            setError('Не удалось очистить историю. Попробуйте еще раз.');
        } finally {
            setLoading(false);
        }
    };

    // Сценарий 1: Пользователь не авторизован
    if (!isAuthenticated) {
        const handleLoginClick = () => {
            // Сохраняем текущий путь с hash для возврата к вкладке
            navigate('/login', { state: { from: location.pathname, tab: 'chat' } });
        };

        const handleRegisterClick = () => {
            navigate('/register', { state: { from: location.pathname, tab: 'chat' } });
        };

        return (
            <div className="chat-container">
                <div className="chat-empty-state" style={{ height: '100%', justifyContent: 'center' }}>
                    <h5>💬 AI Консультант</h5>
                    <p>Для получения консультации от AI ассистента необходимо войти в аккаунт.</p>
                    <div className="mt-3">
                        <Button variant="success" onClick={handleLoginClick} className="me-2">
                            Войти
                        </Button>
                        <Button variant="outline-success" onClick={handleRegisterClick}>
                            Зарегистрироваться
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    // Сценарий 2: Пользователь авторизован, но не является покупателем
    if (!canPurchase) {
        return (
            <div className="chat-container">
                <div className="chat-empty-state" style={{ height: '100%', justifyContent: 'center' }}>
                    <h5>💬 AI Консультант</h5>
                    <p>К сожалению, консультация с AI ассистентом доступна только для пользователей с правами покупателя.</p>
                    <p className="text-muted mt-2">
                        Если вы считаете, что это ошибка, пожалуйста, свяжитесь с администратором.
                    </p>
                </div>
            </div>
        );
    }

    // Сценарий 3: Пользователь авторизован и является покупателем - загрузка чата
    if (loading && !conversation) {
        return (
            <div className="text-center py-5">
                <Spinner animation="border" variant="success" />
                <p className="mt-2">Загрузка чата...</p>
            </div>
        );
    }

    return (
        <div className="chat-container" ref={chatContainerRef}>
            {/* Шапка с кнопкой очистки */}
            <div className="chat-header">
                <h5 className="mb-0">💬 AI Консультант</h5>
                <Button
                    variant="outline-secondary"
                    size="sm"
                    onClick={() => setShowClearModal(true)}
                    disabled={messages.length === 0 || sending}
                >
                    🗑️ Очистить историю
                </Button>
            </div>

            {/* Ошибка */}
            {error && (
                <Alert variant="danger" dismissible onClose={() => setError(null)} className="mx-3 mt-3">
                    {error}
                </Alert>
            )}

            {/* Список сообщений */}
            <div className="chat-messages" ref={messagesContainerRef}>
                {messages.length === 0 ? (
                    <div className="chat-empty-state">
                        <h5>💬 AI Консультант</h5>
                        <p>Здесь вы можете задать вопросы AI ассистенту о данном товаре.</p>
                        <p>Начните разговор, задав свой вопрос!</p>
                    </div>
                ) : (
                    messages.map((message) => (
                        <div
                            key={message.id}
                            className={`chat-message ${message.role.toLowerCase() === 'user' ? 'chat-message-user' : 'chat-message-bot'}`}
                        >
                            {message.role.toLowerCase() === 'assistant' ? (
                                <MarkdownContent content={message.content} />
                            ) : (
                                <div>{message.content}</div>
                            )}
                        </div>
                    ))
                )}

                {/* Индикатор загрузки при отправке */}
                {sending && (
                    <div className="chat-message chat-message-bot">
                        <div className="chat-typing-indicator">
                            <span></span>
                            <span></span>
                            <span></span>
                        </div>
                    </div>
                )}

                <div ref={messagesEndRef} />
            </div>

            {/* Поле ввода */}
            <div className="chat-input-box">
                <Form onSubmit={handleSendMessage} className="d-flex w-100">
                    <Form.Control
                        type="text"
                        placeholder="Задайте вопрос о товаре..."
                        value={inputMessage}
                        onChange={(e) => setInputMessage(e.target.value)}
                        disabled={sending || !conversation}
                        className="chat-input"
                    />
                    <Button
                        type="submit"
                        variant="success"
                        disabled={!inputMessage.trim() || sending || !conversation}
                        className="ms-2 chat-send-button"
                    >
                        {sending ? <Spinner animation="border" size="sm" /> : '➤'}
                    </Button>
                </Form>
            </div>

            {/* Modal подтверждения очистки */}
            <Modal show={showClearModal} onHide={() => setShowClearModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>Очистить историю?</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    Удалить всю историю диалога с AI консультантом? Это действие нельзя отменить.
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowClearModal(false)}>
                        Отмена
                    </Button>
                    <Button variant="danger" onClick={handleClearHistory}>
                        Удалить
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
};

export default AiChatBot;
