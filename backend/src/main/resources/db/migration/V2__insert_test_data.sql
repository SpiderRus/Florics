-- V2__insert_test_data.sql
-- Test data for development and demo purposes
-- Users: alice@example.com, bob@example.com, admin@example.com (password: password123)

-- =====================================================
-- 1. TEST USERS
-- =====================================================
-- Password for all users: "password123"
-- Bcrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

INSERT INTO users (id, name, email, password, roles) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'Alice', 'alice@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', ARRAY['USER', 'BUYER']),
('550e8400-e29b-41d4-a716-446655440001', 'Bob', 'bob@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', ARRAY['USER', 'BUYER']),
('550e8400-e29b-41d4-a716-446655440002', 'Admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', ARRAY['USER', 'ADMIN', 'BUYER']);

-- =====================================================
-- 2. CATEGORIES
-- =====================================================
INSERT INTO categories (id, name, type) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'Комнатные растения', 'PLANT'),
('550e8400-e29b-41d4-a716-446655440011', 'Флорариумы', 'TERRARIUM'),
('550e8400-e29b-41d4-a716-446655440012', 'Мастер-классы', 'COURSE');

-- =====================================================
-- 3. GOODS (Plants)
-- =====================================================
INSERT INTO goods (id, name, description, price, category_id, difficulty, detailed_description, care_instructions) VALUES
(
    '550e8400-e29b-41d4-a716-446655440101',
    'Монстера деликатесная',
    'Популярная тропическая лиана с крупными резными листьями. Быстро растет и создает эффектный акцент в интерьере.',
    1500.00,
    '550e8400-e29b-41d4-a716-446655440010',
    'Легко',
    '## О растении

**Монстера деликатесная** — одно из самых популярных комнатных растений. Родом из тропических лесов Центральной Америки.

### Основные характеристики
- Размер: В домашних условиях 2-3 метра
- Листья: Крупные, с характерными прорезями
- Скорость роста: Быстрая — до 1 метра в год',
    '## Освещение
Яркий рассеянный свет. Избегайте прямых солнечных лучей.

## Полив
Весна-лето: 1-2 раза в неделю. Осень-зима: 1 раз в неделю.

## Температура
Оптимально 18-25°C'
),
(
    '550e8400-e29b-41d4-a716-446655440102',
    'Фикус лировидный',
    'Элегантное растение с крупными листьями в форме скрипки. Прекрасно очищает воздух.',
    2500.00,
    '550e8400-e29b-41d4-a716-446655440010',
    'Средне',
    '## О растении

Фикус лировидный — стильное дерево для современного интерьера.

### Особенности
- Крупные глянцевые листья
- Может вырасти до 2-3 метров
- Хорошо очищает воздух',
    '## Освещение
Яркий рассеянный свет, не менее 6 часов в день.

## Полив
Умеренный полив. Не переливать!'
),
(
    '550e8400-e29b-41d4-a716-446655440103',
    'Сансевиерия (Щучий хвост)',
    'Неубиваемое растение, идеальное для начинающих. Переносит любые условия.',
    800.00,
    '550e8400-e29b-41d4-a716-446655440010',
    'Очень легко',
    '## О растении

Сансевиерия — самое неприхотливое растение в мире!

### Преимущества
- Не требует частого полива
- Растет в тени и на солнце
- Очищает воздух даже ночью',
    '## Освещение
Любое — от тени до яркого солнца.

## Полив
Редко! 1 раз в 2-3 недели летом, 1 раз в месяц зимой.'
);

-- =====================================================
-- 4. GOODS (Terrariums)
-- =====================================================
INSERT INTO goods (id, name, description, price, category_id, difficulty, detailed_description, care_instructions) VALUES
(
    '550e8400-e29b-41d4-a716-446655440201',
    'Флорариум "Тропический лес"',
    'Закрытая экосистема в стеклянной вазе с тропическими мхами и папоротниками.',
    3500.00,
    '550e8400-e29b-41d4-a716-446655440011',
    'Легко',
    '## О флорариуме

Закрытый флорариум — это самоподдерживающаяся экосистема.

### Состав
- Тропические мхи
- Миниатюрные папоротники
- Фиттония
- Декоративные камни',
    '## Полив
Практически не требуется! Раз в 2-3 месяца.

## Освещение
Рассеянный свет. Избегать прямого солнца.'
),
(
    '550e8400-e29b-41d4-a716-446655440202',
    'Флорариум "Пустыня"',
    'Композиция из суккулентов и кактусов в открытой геометрической вазе.',
    2800.00,
    '550e8400-e29b-41d4-a716-446655440011',
    'Очень легко',
    '## О флорариуме

Открытый флорариум с засухоустойчивыми растениями.

### Состав
- Эхеверия
- Крассула
- Хавортия
- Цветной песок',
    '## Полив
Раз в 3-4 недели летом, раз в 1-2 месяца зимой.

## Освещение
Яркий свет, можно прямое солнце.'
);

-- =====================================================
-- 5. GOODS (Master Classes)
-- =====================================================
INSERT INTO goods (id, name, description, price, category_id, difficulty, duration, video_url) VALUES
(
    '550e8400-e29b-41d4-a716-446655440301',
    'Создание флорариума своими руками',
    'Научитесь создавать стильные флорариумы для дома и подарков. Полный курс для начинающих.',
    1200.00,
    '550e8400-e29b-41d4-a716-446655440012',
    'Легко',
    90,
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'
),
(
    '550e8400-e29b-41d4-a716-446655440302',
    'Уход за комнатными растениями',
    'Все секреты правильного ухода: полив, подкормка, пересадка, борьба с вредителями.',
    900.00,
    '550e8400-e29b-41d4-a716-446655440012',
    'Легко',
    60,
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'
);

-- =====================================================
-- 6. MEDIA (Images and Videos for Goods)
-- =====================================================
-- Media for Монстера
INSERT INTO media (goods_id, type, url, display_order) VALUES
('550e8400-e29b-41d4-a716-446655440101', 'IMAGE', 'https://images.unsplash.com/photo-1614594975525-e45190c55d0b?w=400', 0),
('550e8400-e29b-41d4-a716-446655440101', 'IMAGE', 'https://images.unsplash.com/photo-1614594895304-fe7116ac3b58?w=400', 1),
('550e8400-e29b-41d4-a716-446655440101', 'VIDEO', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4', 2);

-- Media for Фикус
INSERT INTO media (goods_id, type, url, display_order) VALUES
('550e8400-e29b-41d4-a716-446655440102', 'IMAGE', 'https://images.unsplash.com/photo-1598880940371-c756e015faf4?w=400', 0),
('550e8400-e29b-41d4-a716-446655440102', 'IMAGE', 'https://images.unsplash.com/photo-1509423350716-97f9360b4e09?w=400', 1);

-- Media for Сансевиерия
INSERT INTO media (goods_id, type, url, display_order) VALUES
('550e8400-e29b-41d4-a716-446655440103', 'IMAGE', 'https://images.unsplash.com/photo-1593691509543-c55fb32d8de5?w=400', 0);

-- Media for Флорариум "Тропический лес"
INSERT INTO media (goods_id, type, url, display_order) VALUES
('550e8400-e29b-41d4-a716-446655440201', 'IMAGE', 'https://images.unsplash.com/photo-1466781783364-36c955e42a7f?w=400', 0),
('550e8400-e29b-41d4-a716-446655440201', 'IMAGE', 'https://images.unsplash.com/photo-1486393995399-9addcc7be4fd?w=400', 1);

-- Media for Флорариум "Пустыня"
INSERT INTO media (goods_id, type, url, display_order) VALUES
('550e8400-e29b-41d4-a716-446655440202', 'IMAGE', 'https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?w=400', 0),
('550e8400-e29b-41d4-a716-446655440202', 'IMAGE', 'https://images.unsplash.com/photo-1485841938031-1bf81239b815?w=400', 1);

-- =====================================================
-- 7. SAMPLE PURCHASES
-- =====================================================
-- Alice bought Монстера and a Master Class
INSERT INTO purchases (user_id, goods_id, price, quantity, purchase_date) VALUES
('550e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440101', 1500.00, 1, CURRENT_TIMESTAMP - INTERVAL '7 days'),
('550e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440301', 1200.00, 1, CURRENT_TIMESTAMP - INTERVAL '5 days');

-- Bob bought Сансевиерия
INSERT INTO purchases (user_id, goods_id, price, quantity, purchase_date) VALUES
('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440103', 800.00, 1, CURRENT_TIMESTAMP - INTERVAL '3 days');

-- =====================================================
-- 8. SAMPLE REVIEWS
-- =====================================================
-- Alice's review for Монстера
INSERT INTO reviews (goods_id, user_id, user_name, rating, comment, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440000', 'Alice', 5,
'Потрясающее растение! Пришло в отличном состоянии, быстро адаптировалось. Уже выпустило новый лист!',
CURRENT_TIMESTAMP - INTERVAL '5 days');

-- Bob's review for Сансевиерия
INSERT INTO reviews (goods_id, user_id, user_name, rating, comment, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440103', '550e8400-e29b-41d4-a716-446655440001', 'Bob', 4,
'Идеально для моего офиса. Неприхотливое и стильное. Минус звезда за небольшой размер.',
CURRENT_TIMESTAMP - INTERVAL '2 days');

-- =====================================================
-- 9. SAMPLE CART ITEMS
-- =====================================================
-- Bob has Фикус in his cart
INSERT INTO cart_items (user_id, goods_id, quantity, added_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440102', 1, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Admin has a Terrarium in cart
INSERT INTO cart_items (user_id, goods_id, quantity, added_at) VALUES
('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440201', 1, CURRENT_TIMESTAMP - INTERVAL '2 hours');
