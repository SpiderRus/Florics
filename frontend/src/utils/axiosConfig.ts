import axios from 'axios';

export const setupAxiosInterceptors = () => {
    axios.interceptors.request.use(
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

    axios.interceptors.response.use(
        (response) => response,
        (error) => {
            if (error.response?.status === 401) {
                // Токен истек или невалиден
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '/login';
            }
            return Promise.reject(error);
        }
    );
};
