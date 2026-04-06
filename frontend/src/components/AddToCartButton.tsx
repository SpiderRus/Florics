import React, { useState } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-toastify';

interface AddToCartButtonProps {
    goodsId: string;
    goodsName: string;
    isMasterClass?: boolean;
}

const AddToCartButton: React.FC<AddToCartButtonProps> = ({ goodsId, goodsName, isMasterClass = false }) => {
    const { addToCart, isInCart } = useCart();
    const { user, isAuthenticated } = useAuth();
    const [quantity, setQuantity] = useState(1);
    const [adding, setAdding] = useState(false);
    const [quantityError, setQuantityError] = useState<string>('');

    // Неавторизованные могут добавлять в локальную корзину
    // Авторизованные без BUYER роли - не могут
    const canAddToCart = !isAuthenticated || user?.canPurchase;

    // Проверяем, есть ли товар уже в корзине
    const alreadyInCart = isInCart(goodsId);

    // Для мастер-классов, которые уже в корзине, блокируем добавление
    const isDisabled = isMasterClass && alreadyInCart;

    const validateQuantity = (value: number): string => {
        if (value < 1) return 'Минимум 1';
        if (value > 99) return 'Максимум 99';
        return '';
    };

    const handleQuantityChange = (value: string) => {
        const numValue = parseInt(value) || 1;
        const clampedValue = Math.max(1, Math.min(99, numValue));
        setQuantity(clampedValue);

        const error = validateQuantity(clampedValue);
        setQuantityError(error);
    };

    const handleAddToCart = async () => {
        // Для мастер-классов не проверяем quantity, всегда 1
        if (!isMasterClass) {
            const error = validateQuantity(quantity);
            if (error) {
                setQuantityError(error);
                return;
            }
        }

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
            // Для мастер-классов всегда добавляем 1 шт
            const quantityToAdd = isMasterClass ? 1 : quantity;
            await addToCart(goodsId, quantityToAdd);

            if (isMasterClass) {
                toast.success(`🛒 ${goodsName} добавлен в корзину`);
            } else {
                toast.success(`🛒 ${goodsName} добавлено в корзину (${quantity} шт)`);
                setQuantity(1);
            }
        } catch (error) {
            toast.error('❌ Ошибка при добавлении в корзину');
        } finally {
            setAdding(false);
        }
    };

    return (
        <div className="add-to-cart-section">
            <div className="add-to-cart-controls">
                {/* Для мастер-классов не показываем поле количества */}
                {!isMasterClass && (
                    <Form.Control
                        type="number"
                        min="1"
                        max="99"
                        value={quantity}
                        onChange={(e) => handleQuantityChange(e.target.value)}
                        isInvalid={!!quantityError}
                        style={{ width: '80px' }}
                    />
                )}
                <Button
                    variant={isDisabled ? "secondary" : "success"}
                    onClick={handleAddToCart}
                    disabled={adding || !canAddToCart || !!quantityError || isDisabled}
                    className="add-to-cart-btn"
                    title={
                        !canAddToCart
                            ? "У вас нет прав на покупку"
                            : isDisabled
                                ? "Уже в корзине"
                                : ""
                    }
                >
                    {isDisabled
                        ? '✓ В корзине'
                        : adding
                            ? 'Добавление...'
                            : '🛒 В корзину'
                    }
                </Button>
            </div>
            {quantityError && !isMasterClass && (
                <small style={{ color: '#dc3545', position: 'absolute', marginTop: '3rem' }}>
                    {quantityError}
                </small>
            )}
        </div>
    );
};

export default AddToCartButton;
