-- =====================================================
-- Добавление типа разговора в ai_conversations
-- =====================================================
-- type разделяет разговоры:
--   GOODS     - консультация по товару (goods_id NOT NULL), значение по умолчанию
--   FLORARIUM - дизайнер флорариумов с генерацией картинок (goods_id NULL)
-- Раньше florarium-разговоры определялись по goods_id IS NULL — теперь явным типом.

ALTER TABLE ai_conversations
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'GOODS'
        CHECK (type IN ('GOODS', 'FLORARIUM'));

-- Бэкфилл: существующие общие разговоры (без привязки к товару) — это florarium
UPDATE ai_conversations SET type = 'FLORARIUM' WHERE goods_id IS NULL;

-- Индекс под запрос «последний florarium-разговор пользователя» (WHERE user_id=? AND type=?)
CREATE INDEX idx_ai_conv_user_type ON ai_conversations(user_id, type);

COMMENT ON COLUMN ai_conversations.type IS 'Тип разговора: GOODS (консультация по товару) | FLORARIUM (дизайнер флорариумов)';
