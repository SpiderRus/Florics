import React, { useState } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useCart } from '../contexts/CartContext';
import { toast } from 'react-toastify';

interface AddToCartButtonProps {
    plantId: string;
    plantName: string;
}

const AddToCartButton: React.FC<AddToCartButtonProps> = ({ plantId, plantName }) => {
    const { addToCart } = useCart();
    const [quantity, setQuantity] = useState(1);
    const [adding, setAdding] = useState(false);

    const handleAddToCart = async () => {
        setAdding(true);

        // Fly-to-cart анимация
        const button = document.activeElement as HTMLElement;
        if (button) {
            const buttonRect = button.getBoundingClientRect();
            const cartIcon = document.querySelector('.cart-icon-container') as HTMLElement;
            const cartRect = cartIcon?.getBoundingClientRect();

            if (cartRect) {
                // Создать клон иконки растения
                const flyingIcon = document.createElement('div');
                flyingIcon.textContent = '🪴';
                flyingIcon.className = 'fly-to-cart';
                flyingIcon.style.cssText = `
                    left: ${buttonRect.left}px;
                    top: ${buttonRect.top}px;
                    font-size: 2rem;
                `;
                flyingIcon.style.setProperty('--fly-x', `${cartRect.left - buttonRect.left}px`);
                flyingIcon.style.setProperty('--fly-y', `${cartRect.top - buttonRect.top}px`);

                document.body.appendChild(flyingIcon);

                // Удалить после анимации
                setTimeout(() => {
                    flyingIcon.remove();
                    // Анимация bounce на иконке корзины
                    cartIcon.classList.add('cart-icon-bounce');
                    setTimeout(() => cartIcon.classList.remove('cart-icon-bounce'), 500);
                }, 800);
            }
        }

        try {
            await addToCart(plantId, quantity);
            toast.success(`🛒 ${plantName} добавлено в корзину (${quantity} шт)`);
            setQuantity(1);
        } catch (error) {
            toast.error('❌ Ошибка при добавлении в корзину');
        } finally {
            setAdding(false);
        }
    };

    return (
        <div className="add-to-cart-section" style={{ marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <Form.Control
                    type="number"
                    min="1"
                    value={quantity}
                    onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                    style={{ width: '80px' }}
                />
                <Button
                    variant="success"
                    onClick={handleAddToCart}
                    disabled={adding}
                    className="add-to-cart-btn"
                >
                    {adding ? 'Добавление...' : '🛒 В корзину'}
                </Button>
            </div>
        </div>
    );
};

export default AddToCartButton;
