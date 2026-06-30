-- =====================================================
-- V4: Заказ кастомного флорариума через корзину
-- =====================================================
-- Кастомный флорариум заказывается из чат-дизайнера (/custom-terrarium): он не является товаром
-- каталога, поэтому у него нет goods_id, а цену проставляет администратор позже.
-- Заказ проходит обычным путём: корзина -> checkout -> purchases.
--
-- Дискриминатор кастомного элемента/заказа: goods_id IS NULL (он же conversation_id IS NOT NULL).

-- -----------------------------------------------------
-- cart_items: переход с составного PK (user_id, goods_id) на суррогатный PK id,
-- чтобы можно было хранить элементы без goods_id (кастомный флорариум).
-- -----------------------------------------------------
ALTER TABLE cart_items ADD COLUMN id VARCHAR(36) NOT NULL DEFAULT gen_random_uuid()::varchar;
ALTER TABLE cart_items DROP CONSTRAINT cart_items_pkey;          -- снять составной PK (user_id, goods_id)
ALTER TABLE cart_items ADD PRIMARY KEY (id);
ALTER TABLE cart_items ALTER COLUMN goods_id DROP NOT NULL;      -- у кастомного элемента нет товара каталога

-- Дедуп обычных товаров (раньше обеспечивался составным PK) — частичным уникальным индексом.
-- Кастомные элементы (goods_id IS NULL) под него не попадают, поэтому их может быть несколько.
CREATE UNIQUE INDEX uq_cart_user_goods ON cart_items(user_id, goods_id) WHERE goods_id IS NOT NULL;

-- Поля кастомного заказа в корзине.
ALTER TABLE cart_items
    ADD COLUMN conversation_id  VARCHAR(36) REFERENCES ai_conversations(conversation_id),
    ADD COLUMN image_url        VARCHAR(2000),
    ADD COLUMN customer_comment TEXT,
    ADD COLUMN contact          VARCHAR(200);

COMMENT ON COLUMN cart_items.conversation_id IS 'AI conversation id (custom florarium item); NULL for catalog goods';
COMMENT ON COLUMN cart_items.image_url IS 'Chosen florarium image proxy URL (custom item)';

-- -----------------------------------------------------
-- purchases: кастомный заказ (нет товара каталога, цену ставит админ, есть статус).
-- -----------------------------------------------------
ALTER TABLE purchases ALTER COLUMN goods_id DROP NOT NULL;       -- кастомный заказ без товара каталога
ALTER TABLE purchases ALTER COLUMN price    DROP NOT NULL;       -- цену проставляет администратор позже
ALTER TABLE purchases
    ADD COLUMN conversation_id  VARCHAR(36) REFERENCES ai_conversations(conversation_id),
    ADD COLUMN image_url        VARCHAR(2000),
    ADD COLUMN customer_comment TEXT,
    ADD COLUMN contact          VARCHAR(200),
    ADD COLUMN status           VARCHAR(20)
        CHECK (status IS NULL OR status IN ('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED'));

-- Индекс для экрана админки (список кастомных заказов по статусу/дате).
CREATE INDEX idx_purchases_custom ON purchases(status, purchase_date DESC)
    WHERE conversation_id IS NOT NULL AND deleted_at IS NULL;

COMMENT ON COLUMN purchases.conversation_id IS 'AI conversation id (custom florarium order); NULL for catalog purchases';
COMMENT ON COLUMN purchases.status IS 'Custom order status: NEW/IN_PROGRESS/DONE/CANCELLED; NULL for catalog purchases';
