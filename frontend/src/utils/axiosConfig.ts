import axios from 'axios';
import { toast } from 'react-toastify';

// Create axios instance
const axiosInstance = axios.create({
    baseURL: '/api'
});

export const setupAxiosInterceptors = () => {
    axiosInstance.interceptors.request.use(
        (config) => {
            // Читаем токен напрямую из localStorage, чтобы избежать race condition с React state
            const token = localStorage.getItem('token');

            // Добавляем токен ко ВСЕМ запросам если он есть (включая публичные endpoint'ы)
            // Бэкенд (OpaqueTokenAuthenticationConverter) сам решит, нужна ли аутентификация
            // Для публичных путей конвертер возвращает Mono.empty() и пропускает запрос без аутентификации
            if (token)
                config.headers.Authorization = `Bearer ${token}`;

            return config;
        },
        (error) => Promise.reject(error)
    );

    axiosInstance.interceptors.response.use(
        (response) => response,
        (error) => {
            if (error.response?.status === 401) {
                // Токен истек или невалиден
                localStorage.removeItem('token');
                localStorage.removeItem('user');

                // Сохраняем текущий URL для возврата после логина (кроме самой страницы логина)
                const currentPath = window.location.pathname;
                if (currentPath !== '/login' && currentPath !== '/register')
                    localStorage.setItem('redirectAfterLogin', currentPath);

                window.location.href = '/login';
            } else if (error.response?.status === 403) {
                // Доступ запрещен
                toast.error('Доступ запрещен. У вас нет прав для выполнения этого действия.');
                // Опционально: редирект на главную для admin страниц
                if (window.location.pathname.startsWith('/admin')) {
                    window.location.href = '/';
                }
            }
            return Promise.reject(error);
        }
    );
};

// Export axios instance as default
export default axiosInstance;
