-- V3__create_goods_type_documents.sql
-- Link goods types (PLANT, TERRARIUM, COURSE) with AI Agent document IDs
-- Each document can only be associated with ONE goods type
-- No soft deletes: when document is deleted from AI Agent, this record should be removed

-- =====================================================
-- GOODS_TYPE_DOCUMENTS TABLE
-- =====================================================
CREATE TABLE goods_type_documents (
    document_id VARCHAR(36) PRIMARY KEY,
    goods_type VARCHAR(30) NOT NULL CHECK (goods_type IN ('PLANT', 'TERRARIUM', 'COURSE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for lookups by goods_type (most common query pattern)
CREATE INDEX idx_goods_type_docs_type ON goods_type_documents(goods_type);

COMMENT ON TABLE goods_type_documents IS 'Links goods types with AI Agent document IDs for RAG queries';
COMMENT ON COLUMN goods_type_documents.document_id IS 'UUID from AI Agent DocumentResponse.id';
COMMENT ON COLUMN goods_type_documents.goods_type IS 'Goods type enum: PLANT, TERRARIUM, COURSE';
COMMENT ON COLUMN goods_type_documents.created_at IS 'When the document was associated with this goods type';
