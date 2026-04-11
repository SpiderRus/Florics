import React, { useState, useEffect } from 'react';
import { Container, Nav, Tab } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { GoodsManagement } from './GoodsManagement';
import { toast } from 'react-toastify';

type AdminTab = 'goods' | 'analytics' | 'categories';

export const AdminPanel: React.FC = () => {
    const [activeTab, setActiveTab] = useState<AdminTab>('goods');
    const { user, isAuthenticated, loading, refreshUser } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        // Обновляем информацию о пользователе с бэкенда при входе на админ панель
        const checkAccess = async () => {
            if (!loading) {
                if (!isAuthenticated) {
                    // Если не залогинен - редирект на логин с возвратом на /admin
                    toast.info('Для доступа к панели администратора необходимо войти в систему');
                    navigate('/login', { state: { from: '/admin' } });
                    return;
                }

                // Если залогинен - обновляем данные пользователя
                await refreshUser();
            }
        };
        checkAccess();
    }, [loading, isAuthenticated]);

    useEffect(() => {
        // Проверяем isAdmin после загрузки пользователя
        if (!loading && isAuthenticated) {
            console.log('AdminPanel check:', { user, isAuthenticated, isAdmin: user?.isAdmin });
            if (user?.isAdmin !== true) {
                console.warn('Access denied: user.isAdmin =', user?.isAdmin);
                toast.error('Доступ запрещен. Требуются права администратора.');
                navigate('/');
            }
        }
    }, [user, isAuthenticated, loading, navigate]);

    // Показываем загрузку пока проверяем права
    if (loading || user?.isAdmin !== true) {
        return (
            <Container className="py-5 text-center">
                <div className="spinner-border" role="status">
                    <span className="visually-hidden">Загрузка...</span>
                </div>
            </Container>
        );
    }

    return (
        <Container fluid className="admin-panel py-4">
            <h2 className="mb-4">Панель администратора</h2>

            <Tab.Container activeKey={activeTab} onSelect={(k) => setActiveTab(k as AdminTab)}>
                <Nav variant="tabs" className="mb-4">
                    <Nav.Item>
                        <Nav.Link eventKey="goods">Товары</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="analytics" disabled>
                            Аналитика <span className="text-muted">(скоро)</span>
                        </Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="categories" disabled>
                            Категории <span className="text-muted">(скоро)</span>
                        </Nav.Link>
                    </Nav.Item>
                </Nav>

                <Tab.Content>
                    <Tab.Pane eventKey="goods">
                        <GoodsManagement />
                    </Tab.Pane>
                    <Tab.Pane eventKey="analytics">
                        <div className="text-center text-muted p-5">
                            Аналитика будет доступна в следующей версии
                        </div>
                    </Tab.Pane>
                    <Tab.Pane eventKey="categories">
                        <div className="text-center text-muted p-5">
                            Управление категориями будет доступно в следующей версии
                        </div>
                    </Tab.Pane>
                </Tab.Content>
            </Tab.Container>
        </Container>
    );
};
