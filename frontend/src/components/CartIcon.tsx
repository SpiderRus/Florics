import React, { useRef, useState, useEffect } from 'react';
import { Badge, Nav } from 'react-bootstrap';
import { useNavigate, useLocation } from 'react-router-dom';
import { useCart } from '../contexts/CartContext';

const CartIcon: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { getTotalItems } = useCart();
    const badgeRef = useRef<HTMLSpanElement>(null);
    const [prevCount, setPrevCount] = useState(0);

    const totalItems = getTotalItems();

    // Pulse анимация при изменении количества
    useEffect(() => {
        if (totalItems > prevCount && badgeRef.current) {
            badgeRef.current.classList.add('badge-pulse');
            setTimeout(() => {
                badgeRef.current?.classList.remove('badge-pulse');
            }, 400);
        }
        setPrevCount(totalItems);
    }, [totalItems, prevCount]);

    const handleCartClick = () => {
        navigate('/cart', {
            state: {
                from: location.pathname
            }
        });
    };

    return (
        <Nav.Link
            onClick={handleCartClick}
            style={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}
            className="cart-icon-container"
        >
            <span style={{ fontSize: '1.2rem' }}>🛒</span>
            {totalItems > 0 && (
                <Badge
                    ref={badgeRef}
                    bg="danger"
                    pill
                    style={{
                        position: 'absolute',
                        top: '0',
                        right: '-5px',
                        fontSize: '0.65rem',
                        minWidth: '18px',
                        height: '18px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}
                >
                    {totalItems}
                </Badge>
            )}
        </Nav.Link>
    );
};

export default CartIcon;
