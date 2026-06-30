import React from 'react';
import { Form, InputGroup } from 'react-bootstrap';
import { Search } from 'react-bootstrap-icons';
import { SortOption } from '../hooks/useGoodsFilter';

interface CatalogToolbarProps {
    query: string;
    onQueryChange: (value: string) => void;
    sortBy: SortOption;
    onSortChange: (value: SortOption) => void;
    resultCount: number;
    totalCount: number;
    searchPlaceholder?: string;
}

const CatalogToolbar: React.FC<CatalogToolbarProps> = ({
    query,
    onQueryChange,
    sortBy,
    onSortChange,
    resultCount,
    totalCount,
    searchPlaceholder = 'Поиск по названию или описанию'
}) => (
    <div className="catalog-toolbar">
        <InputGroup className="catalog-search">
            <InputGroup.Text aria-hidden="true"><Search /></InputGroup.Text>
            <Form.Control
                type="search"
                value={query}
                onChange={(e) => onQueryChange(e.target.value)}
                placeholder={searchPlaceholder}
                aria-label="Поиск по каталогу"
            />
        </InputGroup>

        <Form.Select
            value={sortBy}
            onChange={(e) => onSortChange(e.target.value as SortOption)}
            aria-label="Сортировка товаров"
            className="catalog-sort"
        >
            <option value="default">Сортировка: по умолчанию</option>
            <option value="name-asc">Название: А–Я</option>
            <option value="price-asc">Цена: по возрастанию</option>
            <option value="price-desc">Цена: по убыванию</option>
        </Form.Select>

        <span className="catalog-count text-muted" aria-live="polite">
            {query.trim() ? `Найдено: ${resultCount}` : `Всего: ${totalCount}`}
        </span>
    </div>
);

export default CatalogToolbar;
