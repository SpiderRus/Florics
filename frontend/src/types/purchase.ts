export type PurchaseKind = 'GOODS' | 'CUSTOM_FLORARIUM';

export interface Purchase {
    id: string;
    kind: PurchaseKind;
    goodsId: string | null;          // null для кастомного заказа флорариума
    price: number | null;            // null у кастомного заказа до проставления цены
    purchaseDate: string;
    quantity: number;
    // Поля кастомного заказа флорариума
    conversationId?: string | null;
    imageUrl?: string | null;
    customerComment?: string | null;
    contact?: string | null;
    status?: string | null;          // NEW/IN_PROGRESS/DONE/CANCELLED
}
