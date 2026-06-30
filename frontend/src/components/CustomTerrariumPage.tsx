import React from 'react';
import {Container} from 'react-bootstrap';
import {useAuth} from '../contexts/AuthContext';
import FlorariumChatBot from './FlorariumChatBot';

const CustomTerrariumPage: React.FC = () => {
    const {user, isAuthenticated} = useAuth();

    return (
        <Container className="catalog-page florarium-page">
            <div className="catalog-header">
                <h1>Флорариум под заказ 🪴</h1>
                <p className="florarium-page-subtitle">
                    Опишите флорариум вашей мечты — AI-дизайнер подберёт решение и нарисует, как он может выглядеть.
                </p>
            </div>

            <div className="florarium-chat-wrap">
                <FlorariumChatBot
                    isAuthenticated={isAuthenticated}
                    isBuyer={user?.canPurchase || false}
                />
            </div>
        </Container>
    );
};

export default CustomTerrariumPage;
