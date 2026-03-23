import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { CartSummary, CartItem } from '../types/cart';
import { cartService } from '../services/cartService';
import { plantService } from '../services/plantService';
import { useAuth } from './AuthContext';
import { toast } from 'react-toastify';

interface CartContextType {
    cart: CartSummary | null;
    localCartCount: number;
    localCartItems: CartItem[];
    loading: boolean;
    addToCart: (plantId: string, quantity: number) => Promise<void>;
    updateQuantity: (plantId: string, quantity: number) => Promise<void>;
    removeItem: (plantId: string) => Promise<void>;
    clearCart: () => Promise<void>;
    refreshCart: () => Promise<void>;
    getTotalItems: () => number;
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
            const plants = await plantService.getAllPlants();
            const plantsMap = new Map(plants.map(p => [p.id, p]));

            const items: CartItem[] = localItems
                .map(localItem => {
                    const plant = plantsMap.get(localItem.plantId);
                    if (!plant) return null; // Растение удалено из каталога

                    return {
                        id: `local-${localItem.plantId}`,
                        plant,
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

    const addToCart = async (plantId: string, quantity: number) => {
        if (isAuthenticated) {
            const previousCart = cart;

            // Optimistic update для существующих товаров
            if (cart) {
                const existingItem = cart.items.find(item => item.plant.id === plantId);

                if (existingItem) {
                    // Увеличиваем количество оптимистично
                    const optimisticCart: CartSummary = {
                        items: cart.items.map(item =>
                            item.plant.id === plantId
                                ? { ...item, quantity: item.quantity + quantity }
                                : item
                        ),
                        totalItems: cart.totalItems + quantity,
                        totalPrice: cart.totalPrice + (existingItem.plant.price * quantity)
                    };
                    setCart(optimisticCart);
                }
            }

            try {
                await cartService.addToCart(plantId, quantity);
                // Если товар был новый (не было оптимистичного обновления), загружаем с сервера
                if (cart && !cart.items.find(item => item.plant.id === plantId))
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
            const existingItem = localCartItems.find(item => item.plant.id === plantId);
            if (existingItem) {
                // Увеличиваем количество оптимистично
                const updatedItems = localCartItems.map(item =>
                    item.plant.id === plantId
                        ? { ...item, quantity: item.quantity + quantity }
                        : item
                );
                setLocalCartItems(updatedItems);
            }

            try {
                cartService.addToLocalCart(plantId, quantity);
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

    const updateQuantity = async (plantId: string, quantity: number) => {
        if (isAuthenticated) {
            const previousCart = cart;

            // Optimistic update
            if (cart) {
                if (quantity <= 0) {
                    // Удаление
                    const removedItem = cart.items.find(i => i.plant.id === plantId);
                    if (removedItem) {
                        const optimisticCart: CartSummary = {
                            items: cart.items.filter(item => item.plant.id !== plantId),
                            totalItems: cart.totalItems - removedItem.quantity,
                            totalPrice: cart.totalPrice - (removedItem.quantity * removedItem.plant.price)
                        };
                        setCart(optimisticCart);
                    }
                } else {
                    // Изменение количества
                    const item = cart.items.find(i => i.plant.id === plantId);
                    if (item) {
                        const diff = quantity - item.quantity;
                        const optimisticCart: CartSummary = {
                            items: cart.items.map(cartItem =>
                                cartItem.plant.id === plantId
                                    ? { ...cartItem, quantity }
                                    : cartItem
                            ),
                            totalItems: cart.totalItems + diff,
                            totalPrice: cart.totalPrice + (diff * item.plant.price)
                        };
                        setCart(optimisticCart);
                    }
                }
            }

            try {
                if (quantity <= 0)
                    await cartService.removeItem(plantId);
                else
                    await cartService.updateQuantity(plantId, quantity);
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
                const updatedItems = localCartItems.filter(item => item.plant.id !== plantId);
                setLocalCartItems(updatedItems);
            } else {
                // Изменение количества
                const updatedItems = localCartItems.map(item =>
                    item.plant.id === plantId
                        ? { ...item, quantity }
                        : item
                );
                setLocalCartItems(updatedItems);
            }

            try {
                cartService.updateLocalQuantity(plantId, quantity);
                updateLocalCartCount();
            } catch (error) {
                console.error('Failed to update local quantity:', error);
                setLocalCartItems(previousLocalItems);
                throw error;
            }
        }
    };

    const removeItem = async (plantId: string) => {
        if (isAuthenticated) {
            const previousCart = cart;

            // Optimistic delete
            if (cart) {
                const removedItem = cart.items.find(item => item.plant.id === plantId);
                if (removedItem) {
                    const optimisticCart: CartSummary = {
                        items: cart.items.filter(item => item.plant.id !== plantId),
                        totalItems: cart.totalItems - removedItem.quantity,
                        totalPrice: cart.totalPrice - (removedItem.quantity * removedItem.plant.price)
                    };
                    setCart(optimisticCart);
                }
            }

            try {
                await cartService.removeItem(plantId);
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
            const updatedItems = localCartItems.filter(item => item.plant.id !== plantId);
            setLocalCartItems(updatedItems);

            try {
                cartService.removeFromLocalCart(plantId);
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
                getTotalItems
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
