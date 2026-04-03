import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { Container, Form, Alert } from 'react-bootstrap';
import { useAuth } from '../contexts/AuthContext';

const Register: React.FC = () => {
    const [email, setEmail] = useState('');
    const [name, setName] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [validationErrors, setValidationErrors] = useState<{
        email?: string;
        name?: string;
        password?: string;
        confirmPassword?: string;
    }>({});
    const { register } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const validateEmail = (email: string): string | undefined => {
        if (!email) return 'Email обязателен';
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) return 'Некорректный формат email';
        return undefined;
    };

    const validateName = (name: string): string | undefined => {
        if (!name) return 'Имя обязательно';
        if (name.length < 2) return 'Имя должно содержать минимум 2 символа';
        if (name.length > 100) return 'Имя не может превышать 100 символов';
        return undefined;
    };

    const validatePassword = (password: string): string | undefined => {
        if (!password) return 'Пароль обязателен';
        if (password.length < 8) return 'Пароль должен содержать минимум 8 символов';

        const hasUpperCase = /[A-Z]/.test(password);
        const hasLowerCase = /[a-z]/.test(password);
        const hasDigit = /\d/.test(password);
        const hasSpecial = /[@$!%*?&]/.test(password);

        if (!hasUpperCase) return 'Пароль должен содержать хотя бы одну заглавную букву';
        if (!hasLowerCase) return 'Пароль должен содержать хотя бы одну строчную букву';
        if (!hasDigit) return 'Пароль должен содержать хотя бы одну цифру';
        if (!hasSpecial) return 'Пароль должен содержать хотя бы один спецсимвол (@$!%*?&)';

        return undefined;
    };

    const validateConfirmPassword = (confirmPassword: string): string | undefined => {
        if (!confirmPassword) return 'Подтверждение пароля обязательно';
        if (confirmPassword !== password) return 'Пароли не совпадают';
        return undefined;
    };

    const validateForm = (): boolean => {
        const errors: typeof validationErrors = {};

        const emailError = validateEmail(email);
        if (emailError) errors.email = emailError;

        const nameError = validateName(name);
        if (nameError) errors.name = nameError;

        const passwordError = validatePassword(password);
        if (passwordError) errors.password = passwordError;

        const confirmPasswordError = validateConfirmPassword(confirmPassword);
        if (confirmPasswordError) errors.confirmPassword = confirmPasswordError;

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
            await register(email, name, password);

            // Получаем URL для возврата из state
            const from = (location.state as any)?.from || '/';
            const tab = (location.state as any)?.tab;

            if (tab) {
                navigate(from, { replace: true, state: { tab } });
            } else {
                navigate(from, { replace: true });
            }
        } catch (err: any) {
            setError(err.response?.data?.message || 'Ошибка регистрации');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Container className="d-flex justify-content-center align-items-center" style={{ minHeight: '80vh' }}>
            <div className="auth-card">
                <div className="text-center mb-3">
                    <div className="auth-icon">🪴</div>
                </div>
                <h2 className="text-center mb-4">Регистрация в GreenDecor</h2>
                {error && <Alert variant="danger">{error}</Alert>}
                <Form onSubmit={handleSubmit}>
                    <Form.Group className="mb-3">
                        <Form.Label>Имя</Form.Label>
                        <Form.Control
                            type="text"
                            value={name}
                            onChange={(e) => {
                                setName(e.target.value);
                                setValidationErrors(prev => ({...prev, name: undefined}));
                            }}
                            onBlur={() => {
                                const error = validateName(name);
                                if (error) setValidationErrors(prev => ({...prev, name: error}));
                            }}
                            isInvalid={!!validationErrors.name}
                            required
                        />
                        <Form.Control.Feedback type="invalid">
                            {validationErrors.name}
                        </Form.Control.Feedback>
                        <Form.Text className="text-muted">
                            2-100 символов
                        </Form.Text>
                    </Form.Group>
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
                        <Form.Text className="text-muted">
                            Минимум 8 символов, заглавная буква, строчная буква, цифра, спецсимвол (@$!%*?&)
                        </Form.Text>
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Подтвердите пароль</Form.Label>
                        <Form.Control
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => {
                                setConfirmPassword(e.target.value);
                                setValidationErrors(prev => ({...prev, confirmPassword: undefined}));
                            }}
                            onBlur={() => {
                                const error = validateConfirmPassword(confirmPassword);
                                if (error) setValidationErrors(prev => ({...prev, confirmPassword: error}));
                            }}
                            isInvalid={!!validationErrors.confirmPassword}
                            required
                        />
                        <Form.Control.Feedback type="invalid">
                            {validationErrors.confirmPassword}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <button type="submit" className="auth-submit-button" disabled={loading}>
                        {loading ? 'Регистрация...' : 'Зарегистрироваться'}
                    </button>
                </Form>
                <div className="auth-link-section">
                    Уже есть аккаунт? <Link to="/login">Войти</Link>
                </div>
            </div>
        </Container>
    );
};

export default Register;
