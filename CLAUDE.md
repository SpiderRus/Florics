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
- **Токен-based аутентификация**: Кастомные opaque токены хранятся в базе данных (таблица tokens)
- **Spring Security**: OAuth2 Resource Server с кастомным OpaqueTokenIntrospector
- **Поток токена**: Login → TokenInfo → Opaque token → Сохранение в БД → Валидация при каждом запросе через AuthenticationService
- **Конфигурация**: SecurityConfig.kt определяет публичные/защищенные эндпоинты
- **Frontend**: AuthContext хранит токен в localStorage, axios interceptors добавляют Bearer token
- **Время жизни токена**: Настраивается через `security.token.expiration-hours` (по умолчанию 8760 часов = 1 год)

### Управление состоянием
- **React Context API**: AuthContext (user, token) и CartContext (корзина)
- **Синхронизация корзины**: При логине объединяет localStorage корзину с серверной через `/api/cart/merge`
- **Без Redux**: Простые контексты управляют всем состоянием

### Слой данных
- **PostgreSQL + R2DBC**: Реактивный доступ к БД через Spring Data R2DBC
- **Репозитории**: Двухуровневая архитектура - интерфейсы R2dbcRepository (наследуют CoroutineCrudRepository) и domain-репозитории (UserRepository, GoodsRepository, CartRepository, etc.)
- **Soft deletes**: Все таблицы поддерживают мягкое удаление через колонку `deleted_at`
- **Маппинг**: Mapper классы конвертируют между entity (БД) и model (domain) слоями
- **Миграции**: Flyway управляет схемой БД (V1__create_schema.sql, V2__insert_test_data.sql)
- **Тестовые данные**: Инициализируются через Flyway миграцию V2 при первом запуске

### Маршрутизация
- **Frontend**: React Router DOM обрабатывает SPA-маршруты (/catalog, /terrariums, /masterclasses, /cart, /login, /register)
- **Backend**: RouterConfig.kt возвращает index.html для всех не-API маршрутов (SPA fallback)
- **API префикс**: Все backend эндпоинты используют `/api/*`

### Реактивное программирование
- **WebFlux**: Неблокирующий реактивный стек
- **Kotlin Coroutines**: Backend контроллеры/сервисы используют suspend функции, автоматически конвертируются в Mono/Flux через Spring
- **Context propagation**: Hooks.enableAutomaticContextPropagation() в Application.kt включает распространение SecurityContext в реактивных цепочках

### Структура frontend
- **Компоненты**: Модульные React компоненты в `frontend/src/components/`
- **Сервисы**: API вызовы в `frontend/src/services/` (authService, goodsService, cartService и т.д.)
- **Axios config**: Централизованная конфигурация в `utils/axiosConfig.ts` с interceptors для инъекции токена и обработки ошибок
- **Типы**: TypeScript интерфейсы в `frontend/src/types/`

### Основные backend контроллеры
- **AuthController**: /api/auth/login, /api/auth/register, /api/auth/logout, /api/auth/me
- **GoodsController**: /api/goods (список, фильтрация по категории), /api/goods/{id} (детали)
- **CategoryController**: /api/categories (список всех категорий), /api/categories/{id} (детали категории)
- **CartController**: /api/cart (получить, добавить, обновить, удалить, очистить, объединить)
- **PurchaseController**: /api/purchases (оформление, история пользователя)
- **ReviewController**: /api/reviews/{goodsId} (список, создать), /api/reviews/rating/{goodsId} (средний рейтинг)
- **AiBotController**: /api/aibot/conversations (создать/получить для товара), /api/aibot/chat/{conversationId} (отправить сообщение), /api/aibot/conversations/{conversationId}/messages (история)
- **UserController**: /api/users (CRUD операции с пользователями, только для админов)
- **HelloController**: /api/hello, /api/stream (тестовые эндпоинты)

### Swagger/OpenAPI
Документация API доступна по адресу http://localhost:8080/swagger-ui.html (springdoc-openapi-starter-webflux-ui 3.0.1)

### AI чат-бот интеграция
- **Внешний сервис**: AI Agent работает на http://localhost:8081/api/v1
- **WebClient**: Настраивается через AiAgentProperties и WebClientConfig
- **RAG**: Поддержка контекстного поиска по товарам через векторную БД
- **Conversations**: Каждый пользователь может иметь отдельный разговор для каждого товара
- **Маппинг**: Таблица `ai_conversations` связывает (userId, goodsId) с conversationId
- **Загрузка документов**: Эндпоинты /api/documents/** для загрузки файлов в векторную БД (требует аутентификацию)

### Frontend компоненты
- **Основные страницы**: GoodsCatalog, TerrariumCatalog, MasterClassCatalog, GoodsDetailPage, CartPage, ProfilePage
- **Карточки**: GoodsCard, CourseCard с поддержкой категорий и медиа
- **Медиа**: ImageCarousel, MediaCarousel, LargeMediaCarousel, VideoPlayer, MediaModal для галерей и видео
- **Отзывы**: ReviewList, ReviewForm, StarRating для системы рейтингов
- **AI чат**: AiChatBot для консультаций по товарам (требует аутентификацию и покупку)
- **Markdown**: MarkdownContent с react-markdown и remark-gfm для описаний товаров

## Ключевые паттерны

### SecurityContext в реактивных цепочках
Для доступа к аутентифицированному пользователю в реактивном коде:
```kotlin
SecurityUtils.getAuthenticatedUserId() // Возвращает suspend Long
```
Извлекает userId из SecurityContext (OAuth2AuthenticatedPrincipal).

### Стратегия объединения корзины
При логине клиент отправляет items из localStorage в `/api/cart/merge`. Сервер суммирует количества для дублирующихся товаров и сохраняет в БД.

### CORS и CSRF
- CSRF отключен в SecurityConfig (csrf().disable()) для stateless токенов
- CORS настраивается в WebFluxConfig с поддержкой credentials и всех основных методов

### Интеграция сборки frontend
Maven resources plugin копирует dist из frontend/dist в backend/target/classes/static во время фазы process-resources.

### Категории товаров
- **Типы**: PLANT (растения), TERRARIUM (флорариумы), COURSE (мастер-классы)
- **БД**: Таблица categories с полями id, name, type
- **Связь**: Goods ссылается на category_id (внешний ключ)
- **Frontend маршруты**: /catalog (растения), /terrariums (флорариумы), /masterclasses (курсы)

### Медиа система
- **Таблица media**: Хранит изображения и видео для товаров (type: IMAGE/VIDEO)
- **Поля goods**: preview_url (основное превью), video_url (видео для курсов), detailed_description и care_instructions (markdown)
- **Display order**: Порядок отображения медиа в UI через поле display_order

## Частые задачи

### Добавить новый API эндпоинт
1. Создать DTO в `backend/src/main/kotlin/com/example/webflux/controller/model/Dtos.kt`
2. Создать domain модель (если нужно) в `backend/src/main/kotlin/com/example/webflux/domain/model/`
3. Добавить entity (если работаем с БД) в `backend/src/main/kotlin/com/example/webflux/domain/entity/`
4. Создать/обновить R2DBC репозиторий в `backend/src/main/kotlin/com/example/webflux/repository/r2dbc/`
5. Создать/обновить domain репозиторий в `backend/src/main/kotlin/com/example/webflux/repository/`
6. Добавить метод сервиса в `backend/src/main/kotlin/com/example/webflux/service/`
7. Добавить метод контроллера с @RestController и аннотациями OpenAPI
8. Добавить маршрут в SecurityConfig.kt (public или authenticated)

### Добавить новую frontend страницу
1. Создать компонент в `frontend/src/components/`
2. Добавить сервис (если нужно) в `frontend/src/services/`
3. Добавить типы (если нужно) в `frontend/src/types/`
4. Добавить маршрут в `App.tsx` (<Route path="..." element={...}/>)
5. Добавить запись в SecurityConfig.kt для разрешения SPA fallback для пути

### Изменить схему БД
1. Создать новую миграцию в `backend/src/main/resources/db/migration/` с именем V{N}__description.sql
2. Обновить entity в `backend/src/main/kotlin/com/example/webflux/domain/entity/`
3. Обновить mapper в `backend/src/main/kotlin/com/example/webflux/mapper/`
4. Перезапустить приложение - Flyway автоматически применит миграции

### Добавить тестовые данные
Редактировать `backend/src/main/resources/db/migration/V2__insert_test_data.sql` или создать новую миграцию.

### Изменить правила безопасности
Редактировать блок authorizeExchange в SecurityConfig.kt. Использовать `.permitAll()` для публичных, `.authenticated()` для защищенных эндпоинтов.

### Настроить подключение к БД
Редактировать `backend/src/main/resources/application.yml`:
- `spring.r2dbc.url`: строка подключения R2DBC
- `spring.r2dbc.username`: пользователь БД
- `spring.r2dbc.password`: пароль БД

### Настроить AI Agent сервис
Редактировать `backend/src/main/resources/application.yml`:
- `ai-agent.base-url`: URL AI Agent сервиса
- `ai-agent.base-path`: базовый путь API
- `ai-agent.connect-timeout`: таймаут подключения (мс)
- `ai-agent.read-timeout`: таймаут чтения (мс)

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
- **Jackson Kotlin module**: tools.jackson.module

### Структура БД (основные таблицы)
- **users**: id (VARCHAR UUID), name, email, password (bcrypt), roles (TEXT[]), created_at, updated_at, deleted_at
- **categories**: id (VARCHAR UUID), name, type (PLANT/TERRARIUM/COURSE), created_at, deleted_at
- **goods**: id (VARCHAR UUID), name, description, price (DECIMAL), category_id (FK), difficulty, duration (INT, для курсов), video_url, preview_url, detailed_description (markdown), care_instructions (markdown), created_at, updated_at, deleted_at
- **media**: id (VARCHAR UUID), goods_id (FK), type (IMAGE/VIDEO), url, display_order, created_at, deleted_at
- **cart_items**: user_id (FK), goods_id (FK), quantity, added_at, updated_at (composite primary key: user_id, goods_id)
- **purchases**: id (VARCHAR UUID), user_id (FK), goods_id (FK), quantity, price_at_purchase, purchased_at
- **reviews**: id (VARCHAR UUID), goods_id (FK), user_id (FK), rating (1-5), comment, created_at, updated_at, deleted_at
- **ai_conversations**: user_id (FK), goods_id (FK), conversation_id (UUID из AI Agent), created_at (composite primary key: user_id, goods_id)

### Дизайн-тема
- **Цветовая палитра**: Натуральная зеленая палитра (#2d5016, #4a7c2c, #7fa650), земляные тона (#8b7355), кремовый фон (#f4f1ea)
- **Стиль**: Природная эстетика для магазина растений и флорариумов

### Конфигурация приложения
- **Порт**: 8080 (по умолчанию)
- **Статические ресурсы**: classpath:/static/ с pattern /**
- **Логирование**: INFO для root, DEBUG для com.example.webflux и R2DBC
- **Token expiration**: 8760 часов (1 год) по умолчанию

### Зависимости backend
- spring-boot-starter-webflux (реактивный веб)
- spring-boot-starter-security (OAuth2 Resource Server)
- spring-boot-starter-data-r2dbc (реактивный доступ к БД)
- r2dbc-postgresql + postgresql (драйверы БД)
- flyway-core + flyway-database-postgresql (миграции)
- kotlinx-coroutines-reactor (интеграция корутин с Reactor)
- jackson-module-kotlin (JSON сериализация для Kotlin)
- reactor-kotlin-extensions (Kotlin DSL для Reactor)
- springdoc-openapi-starter-webflux-ui (Swagger UI)
- guava 33.5.0-jre (утилиты Google)
- context-propagation 1.2.0 (распространение контекста в реактивных цепочках)

### Зависимости frontend
- react + react-dom (UI библиотека)
- react-router-dom 7.13.1 (маршрутизация)
- bootstrap 5.3.3 + react-bootstrap 2.10.1 (UI компоненты)
- axios 1.6.7 (HTTP клиент)
- react-markdown 10.1.0 + remark-gfm 4.0.1 (рендеринг markdown)
- react-toastify 11.0.5 (уведомления)
- recharts 3.8.1 (графики, если используется)
