import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { CartSummary, CartItem } from '../types/cart';
import { cartService } from '../services/cartService';
import { goodsService } from '../services/goodsService';
import { useAuth } from './AuthContext';
import { toast } from 'react-toastify';

interface CartContextType {
    cart: CartSummary | null;
    localCartCount: number;
    localCartItems: CartItem[];
    loading: boolean;
    addToCart: (goodsId: string, quantity: number) => Promise<void>;
    updateQuantity: (goodsId: string, quantity: number) => Promise<void>;
    removeItem: (goodsId: string) => Promise<void>;
    clearCart: () => Promise<void>;
    refreshCart: () => Promise<void>;
    getTotalItems: () => number;
    isInCart: (goodsId: string) => boolean;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const { isAuthenticated } = useAuth();
    const [cart, setCart] = useState<CartSummary | null>(null);
    const [localCartCount, setLocalCartCount] = useState(0);
    const [localCartItems, setLocalCartItems] = useState<CartItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [previousAuthState, setPreviousAuthState] = useState(isAuthenticated);

    // Инициализация: загружаем счётчик из localStorage при первом рендере
    useEffect(() => {
        if (!isAuthenticated)
            updateLocalCartCount();
    }, []);

    // КРИТИЧЕСКАЯ ЛОГИКА: Синхронизация при переходе из неавторизованного в авторизованное состояние
    useEffect(() => {
        const authStateChanged = previousAuthState !== isAuthenticated;

        if (authStateChanged) {
            if (isAuthenticated) {
                // Переход: неавторизован -> авторизован (LOGIN)
                syncAndLoadCart();
            } else {
                // Переход: авторизован -> неавторизован (LOGOUT)
                setCart(null);
                updateLocalCartCount();
                loadLocalCartItems();
            }
            setPreviousAuthState(isAuthenticated);
        }
        // Автоматическое обновление после смены состояния убрано
        // refreshCart() вызывается явно из компонентов при необходимости
    }, [isAuthenticated]);

    const syncAndLoadCart = async () => {
        setLoading(true);
        try {
            const localItems = cartService.getLocalCart();

            if (localItems.length > 0) {
                // Есть товары в localStorage - мержим с сервером
                const mergedCart = await cartService.mergeCart(localItems);
                setCart(mergedCart);
                // Очищаем localStorage после успешного merge
                cartService.clearLocalCart();
                setLocalCartCount(0);

                // Уведомление об успешной синхронизации
                toast.success(`🔄 Корзина синхронизирована! Добавлено ${localItems.length} товаров`, {
                    autoClose: 5000
                });
            } else {
                // Нет товаров в localStorage - просто загружаем серверную корзину
                await loadServerCart();
            }
        } catch (error: any) {
            console.error('Failed to sync cart:', error);

            // Проверяем статус ошибки
            if (error.response?.status === 403) {
                // 403 Forbidden - у пользователя нет прав на корзину
                cartService.clearLocalCart();
                setLocalCartCount(0);
                toast.error('❌ У вас нет прав для использования корзины', {
                    autoClose: 7000
                });
            } else {
                // Другие ошибки (сеть, 500 и т.д.) - сохраняем localStorage для повторной попытки
                toast.error('⚠️ Ошибка синхронизации корзины. Локальные данные сохранены.', {
                    autoClose: 5000
                });
                // При ошибке не удаляем localStorage - оставляем для повторной попытки
                await loadServerCart();
            }
        } finally {
            setLoading(false);
        }
    };

    const loadServerCart = async () => {
        setLoading(true);
        try {
            const cartData = await cartService.getCart();
            setCart(cartData);
        } catch (error) {
            console.error('Failed to load cart:', error);
        } finally {
            setLoading(false);
        }
    };

    const updateLocalCartCount = () => {
        setLocalCartCount(cartService.getLocalCartCount());
    };

    const loadLocalCartItems = async () => {
        const localItems = cartService.getLocalCart();
        if (localItems.length === 0) {
            setLocalCartItems([]);
            return;
        }

        setLoading(true);
        try {
            // Загружаем детали растений для каждого товара
            const plants = await goodsService.getAllGoods();
            const plantsMap = new Map(plants.map(p => [p.id, p]));

            const items: CartItem[] = localItems
                .map(localItem => {
                    const goods = plantsMap.get(localItem.goodsId);
                    if (!goods) return null; // Растение удалено из каталога

                    return {
                        id: `local-${localItem.goodsId}`,
                        goods,
                        quantity: localItem.quantity,
                        addedAt: new Date().toISOString()
                    };
                })
                .filter((item): item is CartItem => item !== null);

            setLocalCartItems(items);
        } catch (error) {
            console.error('Failed to load local cart items:', error);
            toast.error('⚠️ Не удалось загрузить детали товаров');
            setLocalCartItems([]);
        } finally {
            setLoading(false);
        }
    };

    const addToCart = async (goodsId: string, quantity: number) => {
        if (isAuthenticated) {
            const previousCart = cart;

            // Optimistic update для существующих товаров
            if (cart) {
                const existingItem = cart.items.find(item => item.goods.id === goodsId);

                if (existingItem) {
                    // Увеличиваем количество оптимистично
                    const optimisticCart: CartSummary = {
                        items: cart.items.map(item =>
                            item.goods.id === goodsId
                                ? { ...item, quantity: item.quantity + quantity }
                                : item
                        ),
                        totalItems: cart.totalItems + quantity,
                        totalPrice: cart.totalPrice + (existingItem.goods.price * quantity)
                    };
                    setCart(optimisticCart);
                }
            }

            try {
                await cartService.addToCart(goodsId, quantity);
                // Если товар был новый (не было оптимистичного обновления), загружаем с сервера
                if (cart && !cart.items.find(item => item.goods.id === goodsId))
                    await loadServerCart();
            } catch (error) {
                console.error('Failed to add to cart:', error);
                // Откат к предыдущему состоянию
                setCart(previousCart);
                throw error;
            }
        } else {
            const previousLocalItems = localCartItems;

            // Optimistic update для localStorage корзины
            const existingItem = localCartItems.find(item => item.goods.id === goodsId);
            if (existingItem) {
                // Увеличиваем количество оптимистично
                const updatedItems = localCartItems.map(item =>
                    item.goods.id === goodsId
                        ? { ...item, quantity: item.quantity + quantity }
                        : item
                );
                setLocalCartItems(updatedItems);
            }

            try {
                cartService.addToLocalCart(goodsId, quantity);
                updateLocalCartCount();
                // Если товар был новый, загружаем детали с сервера
                if (!existingItem)
                    await loadLocalCartItems();
            } catch (error) {
                console.error('Failed to add to local cart:', error);
                setLocalCartItems(previousLocalItems);
                throw error;
            }
        }
    };

    const updateQuantity = async (goodsId: string, quantity: number) => {
        if (isAuthenticated) {
            const previousCart = cart;

            // Optimistic update
            if (cart) {
                if (quantity <= 0) {
                    // Удаление
                    const removedItem = cart.items.find(i => i.goods.id === goodsId);
                    if (removedItem) {
                        const optimisticCart: CartSummary = {
                            items: cart.items.filter(item => item.goods.id !== goodsId),
                            totalItems: cart.totalItems - removedItem.quantity,
                            totalPrice: cart.totalPrice - (removedItem.quantity * removedItem.goods.price)
                        };
                        setCart(optimisticCart);
                    }
                } else {
                    // Изменение количества
                    const item = cart.items.find(i => i.goods.id === goodsId);
                    if (item) {
                        const diff = quantity - item.quantity;
                        const optimisticCart: CartSummary = {
                            items: cart.items.map(cartItem =>
                                cartItem.goods.id === goodsId
                                    ? { ...cartItem, quantity }
                                    : cartItem
                            ),
                            totalItems: cart.totalItems + diff,
                            totalPrice: cart.totalPrice + (diff * item.goods.price)
                        };
                        setCart(optimisticCart);
                    }
                }
            }

            try {
                if (quantity <= 0)
                    await cartService.removeItem(goodsId);
                else
                    await cartService.updateQuantity(goodsId, quantity);
                // НЕ перезагружаем корзину с сервера - оптимистичное обновление уже применено
            } catch (error) {
                console.error('Failed to update quantity:', error);
                // Откат к предыдущему состоянию
                setCart(previousCart);
                toast.error('Не удалось обновить количество');
                throw error;
            }
        } else {
            const previousLocalItems = localCartItems;

            // Optimistic update для localStorage корзины
            if (quantity <= 0) {
                // Удаление
                const updatedItems = localCartItems.filter(item => item.goods.id !== goodsId);
                setLocalCartItems(updatedItems);
            } else {
                // Изменение количества
                const updatedItems = localCartItems.map(item =>
                    item.goods.id === goodsId
                        ? { ...item, quantity }
                        : item
                );
                setLocalCartItems(updatedItems);
            }

            try {
                cartService.updateLocalQuantity(goodsId, quantity);
                updateLocalCartCount();
            } catch (error) {
                console.error('Failed to update local quantity:', error);
                setLocalCartItems(previousLocalItems);
                throw error;
            }
        }
    };

    const removeItem = async (goodsId: string) => {
        if (isAuthenticated) {
            const previousCart = cart;

            // Optimistic delete
            if (cart) {
                const removedItem = cart.items.find(item => item.goods.id === goodsId);
                if (removedItem) {
                    const optimisticCart: CartSummary = {
                        items: cart.items.filter(item => item.goods.id !== goodsId),
                        totalItems: cart.totalItems - removedItem.quantity,
                        totalPrice: cart.totalPrice - (removedItem.quantity * removedItem.goods.price)
                    };
                    setCart(optimisticCart);
                }
            }

            try {
                await cartService.removeItem(goodsId);
                // НЕ перезагружаем корзину с сервера - оптимистичное обновление уже применено
            } catch (error) {
                console.error('Failed to remove item:', error);
                setCart(previousCart);
                toast.error('Не удалось удалить товар');
                throw error;
            }
        } else {
            const previousLocalItems = localCartItems;

            // Optimistic delete для localStorage корзины
            const updatedItems = localCartItems.filter(item => item.goods.id !== goodsId);
            setLocalCartItems(updatedItems);

            try {
                cartService.removeFromLocalCart(goodsId);
                updateLocalCartCount();
            } catch (error) {
                console.error('Failed to remove from local cart:', error);
                setLocalCartItems(previousLocalItems);
                throw error;
            }
        }
    };

    const clearCart = async () => {
        if (isAuthenticated) {
            const previousCart = cart;
            // Оптимистично очищаем корзину
            setCart({ items: [], totalItems: 0, totalPrice: 0 });

            try {
                await cartService.clearCart();
                // Корзина уже пуста, не нужно перезагружать
            } catch (error) {
                console.error('Failed to clear cart:', error);
                setCart(previousCart);
                throw error;
            }
        } else {
            const previousLocalItems = localCartItems;

            // Оптимистично очищаем localStorage корзину
            setLocalCartItems([]);

            try {
                cartService.clearLocalCart();
                updateLocalCartCount();
            } catch (error) {
                console.error('Failed to clear local cart:', error);
                setLocalCartItems(previousLocalItems);
                throw error;
            }
        }
    };

    const refreshCart = async () => {
        if (isAuthenticated)
            await loadServerCart();
        else {
            updateLocalCartCount();
            await loadLocalCartItems();
        }
    };

    const getTotalItems = (): number => {
        return isAuthenticated ? (cart?.totalItems ?? 0) : localCartCount;
    };

    const isInCart = (goodsId: string): boolean => {
        if (isAuthenticated) {
            return cart?.items.some(item => item.goods.id === goodsId) ?? false;
        } else {
            return localCartItems.some(item => item.goods.id === goodsId);
        }
    };

    return (
        <CartContext.Provider
            value={{
                cart,
                localCartCount,
                localCartItems,
                loading,
                addToCart,
                updateQuantity,
                removeItem,
                clearCart,
                refreshCart,
                getTotalItems,
                isInCart
            }}
        >
            {children}
        </CartContext.Provider>
    );
};

export const useCart = () => {
    const context = useContext(CartContext);
    if (!context)
        throw new Error('useCart must be used within CartProvider');
    return context;
};
