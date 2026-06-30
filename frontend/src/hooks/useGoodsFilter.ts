import { useMemo, useState } from 'react';
import { Goods } from '../services/goodsService';

export type SortOption = 'default' | 'name-asc' | 'price-asc' | 'price-desc';

// Клиентский поиск + сортировка по уже загруженному списку товаров.
// Серверная фильтрация/пагинация — отдельный следующий шаг (нужен API).
export const useGoodsFilter = (goods: Goods[]) => {
    const [query, setQuery] = useState('');
    const [sortBy, setSortBy] = useState<SortOption>('default');

    const result = useMemo(() => {
        const q = query.trim().toLowerCase();
        const list = q
            ? goods.filter(g =>
                g.name.toLowerCase().includes(q) ||
                g.description.toLowerCase().includes(q))
            : [...goods];

        switch (sortBy) {
            case 'name-asc':
                list.sort((a, b) => a.name.localeCompare(b.name, 'ru'));
                break;
            case 'price-asc':
                list.sort((a, b) => a.price - b.price);
                break;
            case 'price-desc':
                list.sort((a, b) => b.price - a.price);
                break;
        }
        return list;
    }, [goods, query, sortBy]);

    return { query, setQuery, sortBy, setSortBy, result };
};
