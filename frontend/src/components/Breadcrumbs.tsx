import React from 'react';
import { Breadcrumb } from 'react-bootstrap';
import { Link } from 'react-router-dom';

export interface Crumb {
    label: string;
    to?: string; // без to (или последний элемент) — текущая страница, не ссылка
}

const Breadcrumbs: React.FC<{ items: Crumb[] }> = ({ items }) => (
    <Breadcrumb className="brand-breadcrumb">
        {items.map((c, i) => {
            const isLast = i === items.length - 1;
            return isLast || !c.to ? (
                <Breadcrumb.Item key={i} active>{c.label}</Breadcrumb.Item>
            ) : (
                <Breadcrumb.Item key={i} linkAs={Link} linkProps={{ to: c.to }}>
                    {c.label}
                </Breadcrumb.Item>
            );
        })}
    </Breadcrumb>
);

export default Breadcrumbs;
