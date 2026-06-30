import React, { useState, useEffect, useRef } from 'react';
import { Form, Button, Spinner, Alert, Modal } from 'react-bootstrap';
import { SendFill } from 'react-bootstrap-icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { aiBotService } from '../services/aiBotService';
import { Conversation, Message } from '../types/aibot';
import MarkdownContent from './MarkdownContent';
import LoadingSpinner from './LoadingSpinner';
import { useChatScroll } from '../hooks/useChatScroll';

interface AiChatBotProps {
    goodsId: string;
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

    const chatContainerRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    // Общий автоскролл (прилипание к низу)
    const { containerRef: messagesContainerRef, scrollToBottom, handleScroll } = useChatScroll(messages);

    // Доскролл к низу, когда вкладка чата становится видимой
    useEffect(() => {
        const container = chatContainerRef.current;
        if (!container) return;

        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting && messages.length > 0) {
                        setTimeout(() => scrollToBottom(true), 100);
                    }
                });
            },
            { threshold: 0.1 }
        );

        observer.observe(container);
        return () => observer.disconnect();
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
            } catch (err: any) {
                console.error('Failed to initialize conversation:', err);
                setError('Наш эксперт сейчас не на связи, но мы уже работаем над этим! Пожалуйста, загляните сюда чуть позже — мы будем рады ответить на все ваши вопросы.');
            } finally {
                setLoading(false);
            }
        };

        initConversation();
    }, [goodsId, goodsName, isAuthenticated, canPurchase]);

    // Установить фокус на поле ввода после инициализации чата
    useEffect(() => {
        if (!loading && conversation && inputRef.current) {
            inputRef.current.focus();
        }
    }, [loading, conversation]);

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
        const botMessageId = 'temp-bot-' + Date.now();
        setMessages(prev => [...prev, tempUserMessage]);

        let botInserted = false;
        try {
            // Отправить сообщение и получать ответ потоком (SSE)
            await aiBotService.sendMessageStream(conversation.id, userMessage, (token) => {
                setMessages(prev => {
                    if (!botInserted) {
                        botInserted = true;
                        // Первый токен — добавить наполняющееся сообщение бота
                        return [...prev, {
                            id: botMessageId,
                            conversationId: conversation.id,
                            role: 'assistant',
                            content: token,
                            createdAt: new Date().toISOString()
                        }];
                    }
                    // Дописать токен к сообщению бота
                    return prev.map(m =>
                        m.id === botMessageId ? { ...m, content: m.content + token } : m
                    );
                });
            });

            // Стрим завершился, но ни одного токена не пришло (таймаут/пустой ответ AI)
            if (!botInserted)
                throw new Error('empty');
        } catch (err: any) {
            console.error('Failed to send message:', err);
            // Удаляем оптимистичные сообщения и возвращаем введённый текст для повторной отправки
            setMessages(prev => prev.filter(m => m.id !== tempUserMessage.id && m.id !== botMessageId));
            if (err.message !== 'Unauthorized')
                setInputMessage(userMessage);

            if (err.status === 503)
                setError('AI консультант временно недоступен. Попробуйте позже — ваш вопрос сохранён.');
            else if (err.message === 'empty')
                setError('Консультант не успел ответить. Ваш вопрос сохранён — нажмите «Отправить» ещё раз.');
            else if (err.message !== 'Unauthorized')
                setError('Не удалось отправить сообщение. Ваш вопрос сохранён — попробуйте ещё раз.');
        } finally {
            setSending(false);
            // Вернуть фокус на поле ввода после получения ответа
            setTimeout(() => {
                inputRef.current?.focus();
            }, 100);
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
        return <LoadingSpinner text="Загрузка чата..." />;
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
            <div
                className="chat-messages"
                ref={messagesContainerRef}
                onScroll={handleScroll}
                role="log"
                aria-live="polite"
                aria-relevant="additions text"
                aria-label="История диалога с AI консультантом"
            >
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

                {/* Индикатор «печатает…» — держится, пока бот не вернёт ответ целиком */}
                {sending && (
                    <div className="chat-message chat-message-bot">
                        <div className="chat-typing-indicator" role="status" aria-label="Консультант печатает">
                            <span aria-hidden="true"></span>
                            <span aria-hidden="true"></span>
                            <span aria-hidden="true"></span>
                        </div>
                    </div>
                )}
            </div>

            {/* Поле ввода */}
            <div className="chat-input-box">
                <Form onSubmit={handleSendMessage} className="d-flex w-100">
                    <Form.Control
                        ref={inputRef}
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
                        aria-label="Отправить сообщение"
                    >
                        {sending ? <Spinner animation="border" size="sm" /> : <SendFill aria-hidden="true" />}
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
