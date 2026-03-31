import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User } from '../types/auth';
import { authService } from '../services/authService';
import { setupAxiosInterceptors } from '../utils/axiosConfig';

interface AuthContextType {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    loading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, name: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    // Setup axios interceptors once on mount
    useEffect(() => {
        setupAxiosInterceptors();
    }, []);

    // Load user from localStorage on mount
    useEffect(() => {
        const loadUser = () => {
            const savedToken = localStorage.getItem('token');
            const savedUser = localStorage.getItem('user');

            if (savedToken && savedUser) {
                try {
                    // Восстанавливаем пользователя из localStorage без валидации на сервере
                    // Валидация произойдёт при первом API запросе (если токен истёк, вернётся 401)
                    const userData = JSON.parse(savedUser);
                    setUser(userData);
                    setToken(savedToken);
                } catch (error) {
                    // Ошибка парсинга - очищаем localStorage
                    console.log('Failed to parse saved user data, clearing auth data');
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                    setUser(null);
                    setToken(null);
                }
            }
            setLoading(false);
        };
        loadUser();
    }, []);

    const login = async (email: string, password: string) => {
        const response = await authService.login(email, password);

        // ВАЖНО: Сначала сохраняем токен в localStorage, чтобы interceptor его подхватил
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('user', JSON.stringify(response.user));

        // Затем обновляем state - это вызовет useEffect в CartContext
        setUser(response.user);
        setToken(response.accessToken);
    };

    const register = async (email: string, name: string, password: string) => {
        const response = await authService.register(email, name, password);
        setUser(response.user);
        setToken(response.accessToken);
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('user', JSON.stringify(response.user));
    };

    const logout = async () => {
        if (token) {
            try {
                await authService.logout(token);
            } catch (error) {
                console.error('Logout failed:', error);
            }
        }
        setUser(null);
        setToken(null);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                token,
                isAuthenticated: !!user,
                loading,
                login,
                register,
                logout
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context)
        throw new Error('useAuth must be used within AuthProvider');
    return context;
};
