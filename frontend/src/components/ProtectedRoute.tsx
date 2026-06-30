import React, { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import LoadingSpinner from './LoadingSpinner';

interface ProtectedRouteProps {
    children: ReactNode;
    requireAdmin?: boolean;
}

// Защита маршрутов на уровне роутера: неавторизованных уводим на /login (с возвратом),
// не-админов с админских страниц — на главную. Пока идёт проверка авторизации — спиннер.
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requireAdmin = false }) => {
    const { isAuthenticated, user, loading } = useAuth();
    const location = useLocation();

    if (loading)
        return <LoadingSpinner text="Проверка доступа..." />;

    if (!isAuthenticated)
        return <Navigate to="/login" state={{ from: location.pathname }} replace />;

    if (requireAdmin && !user?.isAdmin)
        return <Navigate to="/" replace />;

    return <>{children}</>;
};

export default ProtectedRoute;
