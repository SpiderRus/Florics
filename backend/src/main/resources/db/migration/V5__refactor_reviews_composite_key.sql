-- V5__refactor_reviews_composite_key.sql
-- Refactor reviews table to use composite primary key (goods_id, user_id) instead of id
-- This enforces the business rule: one review per user per product

-- Step 1: Drop existing constraints and primary key
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uq_reviews_user_goods;
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_pkey;

-- Step 2: Drop the id column (no longer needed with composite key)
ALTER TABLE reviews DROP COLUMN IF EXISTS id;

-- Step 3: Create composite primary key
ALTER TABLE reviews ADD PRIMARY KEY (goods_id, user_id);

-- Step 4: Recreate indexes for optimization
DROP INDEX IF EXISTS idx_reviews_goods;
DROP INDEX IF EXISTS idx_reviews_user;
DROP INDEX IF EXISTS idx_reviews_rating;
DROP INDEX IF EXISTS idx_reviews_created;

CREATE INDEX idx_reviews_goods ON reviews(goods_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_user ON reviews(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_rating ON reviews(goods_id, rating DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_created ON reviews(goods_id, created_at DESC) WHERE deleted_at IS NULL;

-- Add comment explaining the composite key
COMMENT ON CONSTRAINT reviews_pkey ON reviews IS 'Composite primary key: one review per user per product';
