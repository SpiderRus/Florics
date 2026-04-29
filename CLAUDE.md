# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Обзор проекта

GreenDecor (Plant Shop) - интернет-магазин растений, флорариумов и мастер-классов с многомодульной Maven архитектурой:
- **Frontend**: React 18 + TypeScript + Vite, стилизация через Bootstrap 5 и React-Bootstrap
- **Backend**: Spring Boot 4.0.3 WebFlux + Kotlin 2.1.0 + Coroutines (реактивное программирование)
- **База данных**: PostgreSQL с R2DBC (реактивный драйвер), миграции через Flyway
- **AI интеграция**: AI чат-бот для консультаций по товарам (интеграция с внешним AI Agent сервисом)
- Одностраничное приложение (SPA) с клиентской маршрутизацией

## Сборка и разработка

### Полная сборка проекта
```bash
mvn clean install
```
Собирает frontend (npm install + vite build), копирует dist в статические ресурсы backend, упаковывает backend JAR со встроенным frontend.

### Запуск приложения
```bash
cd backend
mvn spring-boot:run
```
Доступно по адресу http://localhost:8080

**Требования**: PostgreSQL должен быть запущен на localhost:5432 с базой данных postgres, схемой florashop, пользователем florashop и паролем spider.

### Разработка frontend (с hot reload)
```bash
cd frontend
npm install
npm run dev
```
Запускает dev-сервер Vite с горячей перезагрузкой модулей на http://localhost:5173.

### Только backend
```bash
cd backend
mvn spring-boot:run
```

### Тестирование
Тесты backend:
```bash
cd backend
mvn test
```

## Архитектура

### Аутентификация и авторизация
- **Токен-based аутентификация**: Кастомные opaque токены (UUID) хранятся в базе данных (таблица tokens)
- **Spring Security**: OAuth2 Resource Server с кастомным OpaqueTokenIntrospector (SecurityConfig.kt)
- **Поток токена**: Login → TokenInfo → Opaque token (UUID) → Сохранение в БД → Валидация при каждом запросе через AuthenticationService
- **Роли**: USER (базовая), BUYER (доступ к корзине/покупкам/отзывам/AI), ADMIN (управление товарами/пользователями)
- **Конфигурация**: SecurityConfig.kt определяет публичные/защищенные эндпоинты через authorizeExchange
- **Frontend**: AuthContext хранит токен в localStorage, axios interceptors добавляют Bearer token
- **Время жизни токена**: Настраивается через `security.token.expiration-hours` (по умолчанию 8760 часов = 1 год)
- **Валидация пароля**: @ValidPassword аннотация (PasswordConstraintValidator.kt) — мин. 8 символов, заглавная, строчная, цифра, спецсимвол

### Управление состоянием
- **React Context API**: AuthContext (user, token) и CartContext (корзина)
- **Синхронизация корзины**: При логине объединяет localStorage корзину с серверной через `/api/cart/merge`
- **Без Redux**: Простые контексты управляют всем состоянием

### Слой данных
- **PostgreSQL + R2DBC**: Реактивный доступ к БД через Spring Data R2DBC
- **Репозитории**: Двухуровневая архитектура — интерфейсы R2dbcRepository (наследуют CoroutineCrudRepository) и domain-репозитории (UserRepository, GoodsRepository, CartRepository и т.д.)
- **Soft deletes**: Большинство таблиц поддерживают мягкое удаление через колонку `deleted_at` (кроме tokens и goods_type_documents)
- **Маппинг**: Mapper классы конвертируют между entity (БД) и model (domain) слоями
- **Миграции**: Flyway управляет схемой БД (V1__create_schema.sql, V2__insert_test_data.sql)
- **Тестовые данные**: Инициализируются через Flyway миграцию V2 при первом запуске

### Маршрутизация
- **Frontend**: React Router DOM обрабатывает SPA-маршруты
- **Backend**: RouterConfig.kt возвращает index.html для всех не-API маршрутов (SPA fallback)
- **API префикс**: Все backend эндпоинты используют `/api/*`

### Реактивное программирование
- **WebFlux**: Неблокирующий реактивный стек
- **Kotlin Coroutines**: Backend контроллеры/сервисы используют suspend функции, автоматически конвертируются в Mono/Flux через Spring
- **Context propagation**: Hooks.enableAutomaticContextPropagation() в Application.kt включает распространение SecurityContext в реактивных цепочках

### Lazy Loading (Frontend)
- Критичные компоненты (HomePage, CartIcon, LoadingSpinner, ErrorBoundary) загружаются сразу
- Все страницы (GoodsCatalog, GoodsDetailPage, TerrariumCatalog, MasterClassCatalog и т.д.) загружаются лениво через `React.lazy()`
- ErrorBoundary ловит ошибки загрузки chunks, Suspense показывает LoadingSpinner

### Структура frontend
- **Компоненты**: Модульные React компоненты в `frontend/src/components/`
- **Сервисы**: API вызовы в `frontend/src/services/` (authService, goodsService, cartService, adminService, aiBotService, reviewService, purchaseService, categoryService)
- **Axios config**: Централизованная конфигурация в `utils/axiosConfig.ts` с interceptors для инъекции токена и обработки ошибок
- **Типы**: TypeScript интерфейсы в `frontend/src/types/`
- **Контексты**: `frontend/src/contexts/` (AuthContext, CartContext)

### Основные backend контроллеры
- **AuthController** `/api/auth`: GET /test, POST /login, POST /register, POST /logout, GET /me
- **GoodsController** `/api/goods`: GET / (все товары), GET /{id} (по ID), GET /type/{type} (по типу: PLANT/TERRARIUM/COURSE)
- **CategoryController** `/api/categories`: GET / (все категории), GET /{id} (по ID)
- **CartController** `/api/cart` (роль BUYER): GET / (корзина), POST /items (добавить), PUT /items/{goodsId} (количество), DELETE /items/{goodsId} (удалить товар), DELETE / (очистить), POST /merge (слияние), POST /checkout (оформить заказ)
- **PurchaseController** `/api/purchases` (роль BUYER): GET / (история), GET /has-purchased/{goodsId} (проверка покупки)
- **ReviewController** `/api/reviews`: GET /{goodsId} (список, публично), GET /rating/{goodsId} (рейтинг, публично), POST / (создать, роль BUYER)
- **AiBotController** `/api/aibot` (роль BUYER): POST /conversations (создать/получить), GET /conversations/by-goods/{goodsId} (по товару), GET /conversations/{conversationId}/messages (история), POST /chat/{conversationId} (отправить), DELETE /conversations/{conversationId} (удалить)
- **AdminGoodsController** `/api/admin/goods` (роль ADMIN): GET / (пагинация: page, size, sortBy, sortOrder), POST / (создать), PUT /{id} (обновить), DELETE /{id} (soft delete)
- **FileUploadController** `/api/admin/files` (роль ADMIN): POST /upload (multipart, сохраняет в static/uploads, возвращает URL)
- **UserController** `/api/users` (роль ADMIN): GET / (все), GET /{id}, POST /, PUT /{id}, DELETE /{id}
- **HelloController** `/api/hello`, `/api/stream` (тестовые эндпоинты, публичные)

### Swagger/OpenAPI
Документация API доступна по адресу http://localhost:8080/swagger-ui.html (springdoc-openapi-starter-webflux-ui 3.0.1)
API-docs: http://localhost:8080/api-docs

### AI чат-бот интеграция
- **Внешний сервис**: AI Agent работает на http://localhost:8081/api/v1
- **WebClient**: Настраивается через AiAgentProperties и WebClientConfig (`@Qualifier("aiAgentWebClient")`)
- **RAG**: useRag=true всегда передаётся при отправке сообщений — поиск по векторной БД
- **Conversations**: Каждый пользователь может иметь отдельный разговор для каждого товара
- **Маппинг**: Таблица `ai_conversations` — PRIMARY KEY conversation_id (UUID от AI Agent), с полем goods_id (nullable)
- **Контекст**: При создании разговора передаётся название товара, категория и detailedDescription
- **Документы**: DocumentUploadService загружает/удаляет документы в AI Agent для RAG (векторизация через Ollama nomic-embed-text, хранилище pgvector)
- **goods_type_documents**: Таблица связывает типы товаров (PLANT/TERRARIUM/COURSE) с document_id из AI Agent

### Frontend компоненты
- **Основные страницы**: HomePage, GoodsCatalog, TerrariumCatalog, MasterClassCatalog, GoodsDetailPage, MasterClassPlayer, CartPage, ProfilePage, CustomTerrariumPage
- **Карточки**: GoodsCard, CourseCard с поддержкой категорий и медиа
- **Медиа**: ImageCarousel, MediaCarousel, LargeMediaCarousel, VideoPlayer, VideoControls, MediaModal, ImageModal, LazyImage
- **Отзывы**: ReviewList, ReviewForm, StarRating для системы рейтингов
- **AI чат**: AiChatBot для консультаций по товарам (требует аутентификацию и покупку)
- **Markdown**: MarkdownContent с react-markdown и remark-gfm для описаний товаров
- **Корзина**: CartIcon (навбар), AddToCartButton, CartPage
- **Утилиты**: ErrorBoundary, LoadingSpinner, MarkdownContent
- **Админ**: AdminPanel, GoodsManagement, GoodsForm в `frontend/src/components/admin/`

## Ключевые паттерны

### SecurityContext в реактивных цепочках
Для доступа к аутентифицированному пользователю в реактивном коде:
```kotlin
SecurityUtils.requireCurrentUserId() // Возвращает String (UUID), бросает исключение если не авторизован
SecurityUtils.getCurrentTokenInfo()  // Возвращает TokenInfo? (токен, userId, email, роли)
```
Извлекает userId из SecurityContext (OAuth2AuthenticatedPrincipal).

### Стратегия объединения корзины
При логине клиент отправляет items из localStorage в `/api/cart/merge`. Сервер суммирует количества для дублирующихся товаров и сохраняет в БД.

### Оформление заказа (Checkout)
POST /api/cart/checkout: создаёт Purchase записи для каждого товара в корзине (с quantity), очищает корзину, возвращает CheckoutResponse с orderId.

### CORS и CSRF
- CSRF отключен в SecurityConfig для stateless токенов
- CORS настраивается в WebFluxConfig с поддержкой credentials и всех основных методов

### Интеграция сборки frontend
Maven resources plugin копирует dist из frontend/dist в backend/target/classes/static во время фазы process-resources.

### Категории товаров
- **Типы**: PLANT (растения), TERRARIUM (флорариумы), COURSE (мастер-классы)
- **БД**: Таблица categories с полями id, name, type
- **Связь**: Goods ссылается на category_id (внешний ключ)
- **Frontend маршруты**: /catalog (растения), /terrariums (флорариумы), /masterclasses (курсы)
- **Фильтрация**: GET /api/goods/type/{type} — фильтрует по GoodsType enum

### Медиа система
- **Таблица media**: Хранит изображения и видео для товаров (type: IMAGE/VIDEO)
- **Превью товара**: Первое изображение из таблицы media (поле preview_url удалено из goods в V1)
- **Поля goods**: video_url (для курсов), detailed_description и care_instructions (markdown)
- **Display order**: Порядок отображения медиа через поле display_order
- **Загрузка файлов**: POST /api/admin/files/upload сохраняет в backend/src/main/resources/static/uploads/, возвращает URL вида /uploads/timestamp_filename

### Пагинация (AdminGoodsController)
GET /api/admin/goods поддерживает: page (от 0), size (5-50), sortBy (name/category/price/created_at), sortOrder (asc/desc)

## Частые задачи

### Добавить новый API эндпоинт
1. Создать DTO в `backend/src/main/kotlin/com/example/webflux/controller/model/Dtos.kt`
2. Создать domain модель (если нужно) в `backend/src/main/kotlin/com/example/webflux/domain/model/Models.kt`
3. Добавить entity (если работаем с БД) в `backend/src/main/kotlin/com/example/webflux/entity/Entities.kt`
4. Создать/обновить R2DBC репозиторий в `backend/src/main/kotlin/com/example/webflux/repository/r2dbc/`
5. Создать/обновить domain репозиторий в `backend/src/main/kotlin/com/example/webflux/repository/`
6. Добавить mapper в `backend/src/main/kotlin/com/example/webflux/mapper/Mappers.kt`
7. Добавить метод сервиса в `backend/src/main/kotlin/com/example/webflux/service/`
8. Добавить метод контроллера с @RestController и аннотациями OpenAPI
9. Добавить маршрут в SecurityConfig.kt (public или authenticated)

### Добавить новую frontend страницу
1. Создать компонент в `frontend/src/components/`
2. Добавить сервис (если нужно) в `frontend/src/services/`
3. Добавить типы (если нужно) в `frontend/src/types/`
4. Добавить маршрут в `App.tsx` — ленивый импорт через `lazy()` + `<Route path="..." element={...}/>`
5. Добавить запись в SecurityConfig.kt для разрешения SPA fallback для пути

### Изменить схему БД
1. Создать новую миграцию в `backend/src/main/resources/db/migration/` с именем V{N}__description.sql
2. Обновить entity в `backend/src/main/kotlin/com/example/webflux/entity/Entities.kt`
3. Обновить mapper в `backend/src/main/kotlin/com/example/webflux/mapper/Mappers.kt`
4. Перезапустить приложение — Flyway автоматически применит миграции

### Добавить тестовые данные
Редактировать `backend/src/main/resources/db/migration/V2__insert_test_data.sql` или создать новую миграцию.

### Изменить правила безопасности
Редактировать блок authorizeExchange в SecurityConfig.kt. Использовать `.permitAll()` для публичных, `.authenticated()` для защищенных, `hasRole('ADMIN')`/`hasRole('BUYER')` через @PreAuthorize.

### Настроить подключение к БД
Редактировать `backend/src/main/resources/application.yml`:
- `spring.r2dbc.url`: строка подключения R2DBC
- `spring.r2dbc.username`: пользователь БД
- `spring.r2dbc.password`: пароль БД

### Настроить AI Agent сервис
Редактировать `backend/src/main/resources/application.yml`:
- `ai-agent.base-url`: URL AI Agent сервиса (по умолчанию http://localhost:8081)
- `ai-agent.base-path`: базовый путь API (по умолчанию /api/v1)
- `ai-agent.connect-timeout`: таймаут подключения в мс (по умолчанию 5000)
- `ai-agent.read-timeout`: таймаут чтения в мс (по умолчанию 30000)
- `ai-agent.pool.max-connections`: максимум соединений (по умолчанию 10)
- `ai-agent.pool.pending-acquire-timeout`: таймаут ожидания из pool в мс (по умолчанию 45000)

## Технические детали

### Версии зависимостей
- **Kotlin**: 2.1.0
- **Spring Boot**: 4.0.3
- **Java**: 17+
- **Node.js**: 20.11.0
- **npm**: 10.2.4
- **React**: 18.2.0
- **TypeScript**: 5.4.2
- **Vite**: 5.1.6
- **PostgreSQL**: требуется R2DBC совместимая версия (14+)
- **springdoc-openapi**: 3.0.1
- **spring-ai-bom**: 1.0.0-M5
- **guava**: 33.5.0-jre
- **context-propagation**: 1.2.0

### Структура БД (основные таблицы)
- **users**: id (VARCHAR(36) UUID), name (VARCHAR(100)), email (VARCHAR(100), UNIQUE), password (VARCHAR(60) bcrypt), roles (TEXT[]), created_at, updated_at, deleted_at
- **categories**: id (VARCHAR(36) UUID), name (VARCHAR(100)), type (VARCHAR(20): PLANT/TERRARIUM/COURSE), created_at, deleted_at
- **goods**: id (VARCHAR(36) UUID), name (VARCHAR(200)), description (TEXT), price (DECIMAL(10,2)), category_id (FK), difficulty (VARCHAR(40)), duration (INT, nullable, для курсов), video_url (VARCHAR(2000), nullable), detailed_description (TEXT, markdown), care_instructions (TEXT, markdown), created_at, updated_at, deleted_at
- **media**: id (VARCHAR(36) UUID), goods_id (FK), type (IMAGE/VIDEO), url (VARCHAR(2000)), display_order (INT), created_at, deleted_at
- **cart_items**: user_id (FK, CASCADE), goods_id (FK, CASCADE), quantity, added_at (composite primary key: user_id+goods_id)
- **purchases**: id (VARCHAR(36) UUID), user_id (FK), goods_id (FK), price (DECIMAL), quantity, purchase_date, deleted_at
- **reviews**: goods_id (FK), user_id (FK), user_name (VARCHAR(100), денормализован), rating (1-5), comment (TEXT), created_at, updated_at, deleted_at (composite PK: goods_id+user_id — один отзыв на пользователя на товар)
- **ai_conversations**: conversation_id (VARCHAR(36) PRIMARY KEY — UUID от AI Agent), user_id (FK), goods_id (FK, nullable), created_at, updated_at
- **goods_type_documents**: document_id (VARCHAR(36) PRIMARY KEY), goods_type (PLANT/TERRARIUM/COURSE), created_at
- **tokens**: token (VARCHAR(36) PRIMARY KEY UUID), user_id (FK, CASCADE), created_at, expires_at

### Дизайн-тема
- **Цветовая палитра**: Натуральная зеленая палитра (#2d5016, #4a7c2c, #7fa650), земляные тона (#8b7355), кремовый фон (#f4f1ea)
- **Стиль**: Природная эстетика для магазина растений и флорариумов

### Конфигурация приложения
- **Порт**: 8080 (по умолчанию)
- **Статические ресурсы**: classpath:/static/ с pattern /**
- **Загруженные файлы**: сохраняются в backend/src/main/resources/static/uploads/ (публично доступны по /uploads/**)
- **Логирование**: INFO для root, DEBUG для com.example.webflux и R2DBC
- **Token expiration**: 8760 часов (1 год) по умолчанию

### Зависимости backend
- spring-boot-starter-webflux (реактивный веб)
- spring-boot-starter-security + spring-security-oauth2-resource-server (аутентификация)
- spring-boot-starter-data-r2dbc (реактивный доступ к БД)
- spring-boot-starter-validation (валидация @ValidPassword, @Email, @NotBlank и т.д.)
- r2dbc-postgresql + postgresql (драйверы БД)
- flyway-core + flyway-database-postgresql (миграции)
- kotlinx-coroutines-reactor (интеграция корутин с Reactor)
- jackson-module-kotlin / spring-boot-jackson (JSON сериализация для Kotlin)
- reactor-kotlin-extensions (Kotlin DSL для Reactor)
- springdoc-openapi-starter-webflux-ui 3.0.1 (Swagger UI)
- guava 33.5.0-jre (утилиты Google)
- context-propagation 1.2.0 (распространение контекста в реактивных цепочках)
- spring-ai-bom 1.0.0-M5 (BOM для AI зависимостей)

### Зависимости frontend
- react + react-dom 18.2.0 (UI библиотека)
- react-router-dom 7.13.1 (маршрутизация)
- bootstrap 5.3.3 + react-bootstrap 2.10.1 (UI компоненты)
- axios 1.6.7 (HTTP клиент)
- react-markdown 10.1.0 + remark-gfm 4.0.1 (рендеринг markdown)
- react-toastify 11.0.5 (уведомления)
- recharts 3.8.1 (графики)

### Frontend маршруты (App.tsx)
- `/` — HomePage
- `/catalog` — GoodsCatalog (растения)
- `/catalog/:id` — GoodsDetailPage
- `/terrariums` — TerrariumCatalog
- `/custom-terrarium` — CustomTerrariumPage
- `/masterclasses` — MasterClassCatalog
- `/masterclass/:id` — MasterClassPlayer
- `/cart` — CartPage
- `/profile` — ProfilePage
- `/login` — Login
- `/register` — Register
- `/admin` — AdminPanel (named export, lazy)
