-- V6__create_media_content.sql
-- Бинарное содержимое фото, хранимых в БД (привязано к строке media).
CREATE TABLE media_content (
    media_id     VARCHAR(36) PRIMARY KEY REFERENCES media(id) ON DELETE CASCADE,
    content      BYTEA        NOT NULL,
    content_type VARCHAR(100) NOT NULL,   -- mime, напр. image/jpeg
    size_bytes   INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE media_content IS 'Бинарное содержимое фото, хранимых в БД (1:1 к media)';
