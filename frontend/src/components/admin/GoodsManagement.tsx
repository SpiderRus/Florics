import React, { useEffect, useState } from 'react';
import { Button, Table, Modal, Pagination, Form } from 'react-bootstrap';
import { adminService } from '../../services/adminService';
import { GoodsForm } from './GoodsForm';
import { toast } from 'react-toastify';
import type { Goods } from '../../types/goods';
import type { SortField, SortOrder } from '../../types/admin';

export const GoodsManagement: React.FC = () => {
    const [goods, setGoods] = useState<Goods[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingGoods, setEditingGoods] = useState<Goods | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<Goods | null>(null);

    // Pagination state
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [sortField, setSortField] = useState<SortField>('created_at');
    const [sortOrder, setSortOrder] = useState<SortOrder>('desc');

    useEffect(() => {
        loadGoods();
    }, [currentPage, pageSize, sortField, sortOrder]);

    const loadGoods = async () => {
        setLoading(true);
        try {
            const response = await adminService.getAllGoods({
                page: currentPage,
                size: pageSize,
                sortBy: sortField,
                sortOrder: sortOrder
            });

            setGoods(response.content);
            setTotalPages(response.totalPages);
            setTotalElements(response.totalElements);
        } catch (error) {
            toast.error('Ошибка загрузки товаров');
        } finally {
            setLoading(false);
        }
    };

    const handleSort = (field: SortField) => {
        if (sortField === field) {
            // Toggle sort order if clicking same field
            setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
        } else {
            // New field, default to ascending
            setSortField(field);
            setSortOrder('asc');
        }
        setCurrentPage(0); // Reset to first page on sort change
    };

    const getSortIcon = (field: SortField) => {
        if (sortField !== field) return ' ⇅';
        return sortOrder === 'asc' ? ' ↑' : ' ↓';
    };

    const handlePageChange = (page: number) => {
        setCurrentPage(page);
    };

    const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setPageSize(Number(e.target.value));
        setCurrentPage(0); // Reset to first page
    };

    const handleCreate = () => {
        setEditingGoods(null);
        setShowModal(true);
    };

    const handleEdit = (item: Goods) => {
        setEditingGoods(item);
        setShowModal(true);
    };

    const handleDelete = (item: Goods) => setDeleteTarget(item);

    const confirmDelete = async () => {
        if (!deleteTarget) return;
        try {
            await adminService.deleteGoods(deleteTarget.id);
            toast.success('Товар удалён');
            setDeleteTarget(null);
            loadGoods();
        } catch (error) {
            toast.error('Ошибка удаления товара');
        }
    };

    const handleFormSubmit = async () => {
        setShowModal(false);
        await loadGoods();
    };

    if (loading && currentPage === 0) {
        return <div className="text-center p-5">Загрузка...</div>;
    }

    return (
        <>
            <div className="admin-toolbar d-flex justify-content-between align-items-center gap-3 mb-4">
                <h4 className="mb-0">Управление товарами ({totalElements} товаров)</h4>
                <div className="admin-toolbar-controls d-flex gap-2">
                    <Form.Select
                        value={pageSize}
                        onChange={handlePageSizeChange}
                        style={{ width: 'auto' }}
                    >
                        <option value={10}>10 на странице</option>
                        <option value={20}>20 на странице</option>
                        <option value={50}>50 на странице</option>
                    </Form.Select>
                    <Button variant="success" onClick={handleCreate}>
                        + Добавить товар
                    </Button>
                </div>
            </div>

            <Table striped bordered hover className="rtable">
                <thead>
                    <tr>
                        <th
                            style={{ cursor: 'pointer', userSelect: 'none' }}
                            onClick={() => handleSort('name')}
                        >
                            Название{getSortIcon('name')}
                        </th>
                        <th
                            style={{ cursor: 'pointer', userSelect: 'none' }}
                            onClick={() => handleSort('category')}
                        >
                            Категория{getSortIcon('category')}
                        </th>
                        <th
                            style={{ cursor: 'pointer', userSelect: 'none' }}
                            onClick={() => handleSort('price')}
                        >
                            Цена{getSortIcon('price')}
                        </th>
                        <th>Действия</th>
                    </tr>
                </thead>
                <tbody>
                    {goods.length === 0 ? (
                        <tr>
                            <td colSpan={4} className="text-center text-muted">
                                Товары не найдены
                            </td>
                        </tr>
                    ) : (
                        goods.map(item => (
                            <tr key={item.id}>
                                <td data-label="Название">{item.name}</td>
                                <td data-label="Категория">{item.category?.name || '-'}</td>
                                <td data-label="Цена">{item.price} ₽</td>
                                <td className="rtable-actions">
                                    <div className="d-flex gap-2">
                                        <Button
                                            variant="primary"
                                            size="sm"
                                            onClick={() => handleEdit(item)}
                                        >
                                            Редактировать
                                        </Button>
                                        <Button
                                            variant="danger"
                                            size="sm"
                                            onClick={() => handleDelete(item)}
                                        >
                                            Удалить
                                        </Button>
                                    </div>
                                </td>
                            </tr>
                        ))
                    )}
                </tbody>
            </Table>

            {totalPages > 1 && (
                <div className="d-flex justify-content-center mt-4">
                    <Pagination>
                        <Pagination.First
                            onClick={() => handlePageChange(0)}
                            disabled={currentPage === 0}
                        />
                        <Pagination.Prev
                            onClick={() => handlePageChange(currentPage - 1)}
                            disabled={currentPage === 0}
                        />

                        {[...Array(totalPages)].map((_, idx) => {
                            // Show ellipsis for large page counts
                            if (
                                totalPages > 7 &&
                                idx > 2 &&
                                idx < totalPages - 3 &&
                                Math.abs(idx - currentPage) > 2
                            ) {
                                if (idx === 3 || idx === totalPages - 4) {
                                    return <Pagination.Ellipsis key={idx} disabled />;
                                }
                                return null;
                            }

                            return (
                                <Pagination.Item
                                    key={idx}
                                    active={idx === currentPage}
                                    onClick={() => handlePageChange(idx)}
                                >
                                    {idx + 1}
                                </Pagination.Item>
                            );
                        })}

                        <Pagination.Next
                            onClick={() => handlePageChange(currentPage + 1)}
                            disabled={currentPage === totalPages - 1}
                        />
                        <Pagination.Last
                            onClick={() => handlePageChange(totalPages - 1)}
                            disabled={currentPage === totalPages - 1}
                        />
                    </Pagination>
                </div>
            )}

            <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>
                        {editingGoods ? 'Редактирование товара' : 'Создание товара'}
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <GoodsForm
                        goods={editingGoods}
                        onSuccess={handleFormSubmit}
                        onCancel={() => setShowModal(false)}
                    />
                </Modal.Body>
            </Modal>

            <Modal show={!!deleteTarget} onHide={() => setDeleteTarget(null)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>Удалить товар?</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    Товар «{deleteTarget?.name}» будет удалён. Это действие нельзя отменить.
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setDeleteTarget(null)}>
                        Отмена
                    </Button>
                    <Button variant="danger" onClick={confirmDelete}>
                        Удалить
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
};
