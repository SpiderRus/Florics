-- V1__create_schema.sql
-- Initial database schema for GreenDecor plant shop
-- Tables: users, categories, goods, media, cart_items, purchases, reviews, ai_conversations
-- All tables support soft deletes via deleted_at column

-- =====================================================
-- 1. USERS TABLE
-- =====================================================
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    roles TEXT[] NOT NULL DEFAULT ARRAY['USER'],
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_deleted ON users(deleted_at) WHERE deleted_at IS NOT NULL;

COMMENT ON TABLE users IS 'Users of the plant shop application';
COMMENT ON COLUMN users.roles IS 'Array of user roles: USER, ADMIN, BUYER';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp - NULL means active record';

-- =====================================================
-- 2. CATEGORIES TABLE
-- =====================================================
CREATE TABLE categories (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('PLANT', 'TERRARIUM', 'COURSE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

-- Indexes for categories
CREATE INDEX idx_categories_type ON categories(type) WHERE deleted_at IS NULL;

COMMENT ON TABLE categories IS 'Product categories (plants, terrariums, master classes)';
COMMENT ON COLUMN categories.type IS 'Category type enum: PLANT, TERRARIUM, COURSE';

-- =====================================================
-- 3. GOODS TABLE
-- =====================================================
CREATE TABLE goods (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    name VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    category_id VARCHAR(36) NOT NULL REFERENCES categories(id),
    difficulty VARCHAR(50) NOT NULL,
    duration INT CHECK (duration IS NULL OR duration > 0),
    video_url VARCHAR(1000),
    preview_url VARCHAR(1000),
    detailed_description TEXT,
    care_instructions TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

-- Indexes for goods
CREATE INDEX idx_goods_category ON goods(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_goods_price ON goods(price) WHERE deleted_at IS NULL;
CREATE INDEX idx_goods_name ON goods(name) WHERE deleted_at IS NULL;
CREATE INDEX idx_goods_created ON goods(created_at DESC) WHERE deleted_at IS NULL;

COMMENT ON TABLE goods IS 'Products: plants, terrariums, and master classes';
COMMENT ON COLUMN goods.duration IS 'Duration in minutes (for COURSE type only)';
COMMENT ON COLUMN goods.detailed_description IS 'Markdown formatted detailed description';
COMMENT ON COLUMN goods.care_instructions IS 'Markdown formatted care instructions';

-- =====================================================
-- 4. MEDIA TABLE
-- =====================================================
CREATE TABLE media (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    goods_id VARCHAR(36) NOT NULL REFERENCES goods(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('IMAGE', 'VIDEO')),
    url VARCHAR(1000) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

-- Indexes for media
CREATE INDEX idx_media_goods ON media(goods_id, display_order) WHERE deleted_at IS NULL;

COMMENT ON TABLE media IS 'Media files (images and videos) associated with goods';
COMMENT ON COLUMN media.display_order IS 'Order of display in UI (0 = first)';

-- =====================================================
-- 5. CART_ITEMS TABLE
-- =====================================================
CREATE TABLE cart_items (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goods_id VARCHAR(36) NOT NULL REFERENCES goods(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, goods_id)
);

-- Indexes for cart_items
CREATE INDEX idx_cart_user ON cart_items(user_id);
CREATE INDEX idx_cart_goods ON cart_items(goods_id);

COMMENT ON TABLE cart_items IS 'Shopping cart items (composite PK: user_id + goods_id)';
COMMENT ON COLUMN cart_items.quantity IS 'Quantity of this item in cart';

-- =====================================================
-- 6. PURCHASES TABLE
-- =====================================================
CREATE TABLE purchases (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    goods_id VARCHAR(36) NOT NULL REFERENCES goods(id),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    purchase_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

-- Indexes for purchases
CREATE INDEX idx_purchases_user ON purchases(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchases_goods ON purchases(goods_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchases_user_goods ON purchases(user_id, goods_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchases_date ON purchases(purchase_date DESC) WHERE deleted_at IS NULL;

COMMENT ON TABLE purchases IS 'Purchase history records';
COMMENT ON COLUMN purchases.price IS 'Price at the time of purchase (denormalized for history)';

-- =====================================================
-- 7. REVIEWS TABLE
-- =====================================================
CREATE TABLE reviews (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::varchar,
    goods_id VARCHAR(36) NOT NULL REFERENCES goods(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    user_name VARCHAR(255) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_reviews_user_goods UNIQUE (goods_id, user_id)
);

-- Indexes for reviews
CREATE INDEX idx_reviews_goods ON reviews(goods_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_user ON reviews(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_rating ON reviews(goods_id, rating DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_created ON reviews(goods_id, created_at DESC) WHERE deleted_at IS NULL;

COMMENT ON TABLE reviews IS 'Product reviews and ratings';
COMMENT ON COLUMN reviews.user_name IS 'Denormalized user name for performance';
COMMENT ON COLUMN reviews.rating IS 'Rating from 1 to 5 stars';
COMMENT ON CONSTRAINT uq_reviews_user_goods ON reviews IS 'One review per user per product';

-- =====================================================
-- 8. AI_CONVERSATIONS TABLE
-- =====================================================
CREATE TABLE ai_conversations (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goods_id VARCHAR(36) NOT NULL REFERENCES goods(id) ON DELETE CASCADE,
    conversation_id VARCHAR(36) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, goods_id)
);

-- Indexes for ai_conversations
CREATE INDEX idx_ai_conv_conversation ON ai_conversations(conversation_id);
CREATE INDEX idx_ai_conv_user ON ai_conversations(user_id);
CREATE INDEX idx_ai_conv_goods ON ai_conversations(goods_id);

COMMENT ON TABLE ai_conversations IS 'Mapping of AI conversations to users and goods';
COMMENT ON COLUMN ai_conversations.conversation_id IS 'External conversation ID from AIAgent service';
COMMENT ON CONSTRAINT ai_conversations_pkey ON ai_conversations IS 'One conversation per user per product';
