# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Обзор проекта

GreenDecor - интернет-магазин товаров и флорариумов с многомодульной Maven архитектурой:
- **Frontend**: React 18 + TypeScript + Vite, стилизация через Bootstrap 5
- **Backend**: Spring Boot 4 WebFlux + Kotlin + Coroutines (реактивное программирование)
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

### Разработка frontend (с hot reload)
```bash
cd frontend
npm install
npm run dev
```
Запускает dev-сервер Vite с горячей перезагрузкой модулей.

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
- **Токен-based аутентификация**: Кастомные opaque токены хранятся в памяти (TokenStorage)
- **Spring Security**: OAuth2 Resource Server с кастомным OpaqueTokenIntrospector
- **Поток токена**: Login → TokenInfo → Opaque token → Сохранение в TokenStorage → Валидация при каждом запросе
- **Конфигурация**: SecurityConfig.kt определяет публичные/защищенные эндпоинты
- **Frontend**: AuthContext хранит токен в localStorage, axios interceptors добавляют Bearer token

### Управление состоянием
- **React Context API**: AuthContext (user, token) и CartContext (корзина)
- **Синхронизация корзины**: При логине объединяет localStorage корзину с серверной через `/api/cart/merge`
- **Без Redux**: Простые контексты управляют всем состоянием

### Слой данных
- **In-memory репозитории**: UserRepository, GoodsRepository, CartRepository, PurchaseRepository, ReviewRepository используют ConcurrentHashMap
- **Без БД**: Все данные в памяти и теряются при перезапуске
- **Тестовые учетки**: alice@example.com / bob@example.com / admin@example.com, пароль: "password123"

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
- **GoodsController**: /api/goods (список), /api/goods/{id} (детали)
- **CartController**: /api/cart (получить, добавить, обновить, удалить, очистить, объединить)
- **PurchaseController**: /api/purchases (оформление, история)
- **ReviewController**: /api/reviews/{goodsId} (список, создать, рейтинг)

### Swagger/OpenAPI
Документация API доступна по адресу http://localhost:8080/swagger-ui.html

## Ключевые паттерны

### SecurityContext в реактивных цепочках
Для доступа к аутентифицированному пользователю в реактивном коде:
```kotlin
SecurityUtils.getAuthenticatedUserId() // Возвращает suspend Long
```
Извлекает userId из SecurityContext (OAuth2AuthenticatedPrincipal).

### Стратегия объединения корзины
При логине клиент отправляет items из localStorage в `/api/cart/merge`. Сервер суммирует количества для дублирующихся товаров.

### CORS и CSRF
CSRF отключен для stateless токенов в JWT-стиле. CORS обрабатывается WebFlux (вероятно в WebFluxConfig).

### Интеграция сборки frontend
Maven frontend-plugin устанавливает Node 20.11.0, запускает npm install & build, копирует dist в backend/src/main/resources/static во время Maven сборки.

## Частые задачи

### Добавить новый API эндпоинт
1. Создать DTO в `backend/.../controller/model/`
2. Добавить метод сервиса в `backend/.../service/`
3. Добавить метод контроллера в `backend/.../controller/` с @RestController
4. Добавить маршрут в SecurityConfig.kt (public или authenticated)

### Добавить новую frontend страницу
1. Создать компонент в `frontend/src/components/`
2. Добавить маршрут в `App.tsx` (<Route path="..." element={...}/>)
3. Добавить запись в SecurityConfig.kt для разрешения SPA fallback для пути

### Изменить данные товаров
Редактировать in-memory данные в GoodsRepository.kt (инициализация map goods).

### Изменить правила безопасности
Редактировать блок authorizeExchange в SecurityConfig.kt. Использовать `.permitAll()` для публичных, `.authenticated()` для защищенных эндпоинтов.

## Технические детали

- **Версия Kotlin**: 2.1.0
- **Версия Spring Boot**: 4.0.3
- **Версия Java**: 17+
- **Версия Node**: 20.11.0 (автоматически устанавливается Maven)
- **Дизайн-тема**: Натуральная зеленая палитра (#2d5016, #4a7c2c, #7fa650), земляные тона (#8b7355), кремовый фон (#f4f1ea)
- **Тестовые пользователи**: alice@example.com, bob@example.com, admin@example.com (все с паролем "password123")
