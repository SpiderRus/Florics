import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { Container, Form, Alert } from 'react-bootstrap';
import { useAuth } from '../contexts/AuthContext';

const Login: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [validationErrors, setValidationErrors] = useState<{email?: string, password?: string}>({});
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const validateEmail = (email: string): string | undefined => {
        if (!email) return 'Email обязателен';
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) return 'Некорректный формат email';
        return undefined;
    };

    const validatePassword = (password: string): string | undefined => {
        if (!password) return 'Пароль обязателен';
        if (password.length < 8) return 'Пароль должен содержать минимум 8 символов';
        if (password.length > 128) return 'Пароль не может превышать 128 символов';
        return undefined;
    };

    const validateForm = (): boolean => {
        const errors: {email?: string, password?: string} = {};

        const emailError = validateEmail(email);
        if (emailError) errors.email = emailError;

        const passwordError = validatePassword(password);
        if (passwordError) errors.password = passwordError;

        setValidationErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!validateForm()) {
            return;
        }

        setLoading(true);

        try {
            await login(email, password);

            // Получаем URL для возврата из state или localStorage
            const from = (location.state as any)?.from || localStorage.getItem('redirectAfterLogin') || '/';
            const tab = (location.state as any)?.tab;
            localStorage.removeItem('redirectAfterLogin');

            if (tab) {
                navigate(from, { replace: true, state: { tab } });
            } else {
                navigate(from, { replace: true });
            }
        } catch (err) {
            setError('Неверный email или пароль');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Container className="d-flex justify-content-center align-items-center" style={{ minHeight: '80vh' }}>
            <div className="auth-card">
                <div className="text-center mb-3">
                    <div className="auth-icon">🌿</div>
                </div>
                <h2 className="text-center mb-4">Вход в GreenDecor</h2>
                {error && <Alert variant="danger">{error}</Alert>}
                <Form onSubmit={handleSubmit}>
                    <Form.Group className="mb-3">
                        <Form.Label>Email</Form.Label>
                        <Form.Control
                            type="email"
                            value={email}
                            onChange={(e) => {
                                setEmail(e.target.value);
                                setValidationErrors(prev => ({...prev, email: undefined}));
                            }}
                            onBlur={() => {
                                const error = validateEmail(email);
                                if (error) setValidationErrors(prev => ({...prev, email: error}));
                            }}
                            isInvalid={!!validationErrors.email}
                            required
                        />
                        <Form.Control.Feedback type="invalid">
                            {validationErrors.email}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Пароль</Form.Label>
                        <Form.Control
                            type="password"
                            value={password}
                            onChange={(e) => {
                                setPassword(e.target.value);
                                setValidationErrors(prev => ({...prev, password: undefined}));
                            }}
                            onBlur={() => {
                                const error = validatePassword(password);
                                if (error) setValidationErrors(prev => ({...prev, password: error}));
                            }}
                            isInvalid={!!validationErrors.password}
                            required
                        />
                        <Form.Control.Feedback type="invalid">
                            {validationErrors.password}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <button type="submit" className="auth-submit-button" disabled={loading}>
                        {loading ? 'Вход...' : 'Войти'}
                    </button>
                </Form>
                <div className="auth-link-section">
                    Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
                </div>
            </div>
        </Container>
    );
};

export default Login;
