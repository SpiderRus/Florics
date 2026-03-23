# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Plant Shop (GreenDecor) is a full-stack web application with:
- **Backend**: Spring Boot WebFlux + Kotlin + Coroutines (reactive/async)
- **Frontend**: React + TypeScript + Vite + Bootstrap
- **Build**: Maven multi-module project with integrated frontend build

The application demonstrates reactive programming patterns using Kotlin coroutines with Spring WebFlux.

**Project Statistics:**
- **Backend**: 60 Kotlin files, ~2,324 lines of code
- **Frontend**: 18 TypeScript/TSX files, ~1,544 lines of code
- **Total**: ~3,868 lines of production code
- **Build output**: 38 MB fat JAR (includes frontend)
- **Plant catalog**: 8 products with real Unsplash images

## Build & Run Commands

### Full Build (Maven)
```bash
# Build entire project (frontend + backend)
mvn clean install

# Run backend (after full build)
cd backend
mvn spring-boot:run
```

### Backend Development
```bash
cd backend

# Compile Kotlin sources
mvn kotlin:compile

# Run tests
mvn test

# Run application (port 8080)
mvn spring-boot:run
```

### Frontend Development
```bash
cd frontend

# Install dependencies
npm install

# Development server with hot reload (port 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Quick Rebuild After Frontend Changes
```bash
# If you modified frontend code and need to update backend:
cd frontend && npm run build && cd ../backend && mvn process-resources

# Or rebuild everything:
mvn clean install
```

## Architecture & Key Patterns

### Backend Architecture

**Reactive Stack**: Uses Spring WebFlux with Kotlin coroutines instead of traditional blocking I/O. Controllers use `suspend` functions for async operations and `Flow<T>` for streaming responses.

**Package Structure**:
- `com.example.webflux.controller` - REST controllers with coroutine support (5 controllers)
  - `HelloController` - Demo endpoints (greeting, SSE stream)
  - `PlantController` - Public plant catalog API
  - `AuthController` - Login, register, logout, me
  - `CartController` - Shopping cart operations (all protected)
  - `UserController` - User management (protected/admin)
- `com.example.webflux.config` - Configuration classes (5 configs)
  - `SecurityConfig` - Spring Security + JWT Opaque Token setup
  - `WebFluxConfig` - CORS + static resources
  - `RouterConfig` - SPA routing fallback to index.html
  - `OpenApiConfig` - Swagger/OpenAPI documentation
  - `JacksonConfiguration` - Kotlin serialization support
- `com.example.webflux.security` - Authentication and authorization (9 components)
  - `OpaqueTokenAuthenticationConverter` - Extract Bearer token
  - `OpaqueTokenAuthenticationManager` - Validate token
  - `OpaqueTokenAuthenticationToken` - Custom auth principal
  - `LocalAuthenticationService` - Login/register with BCrypt
  - `LocalAuthorizationService` - RBAC role checking
  - `TokenStorage` - In-memory token storage (ConcurrentHashMap)
  - `SecurityUtils` - Get current user from context
- `com.example.webflux.repository` - In-memory data repositories (3 repositories)
  - `PlantRepository` - 8 plants with images, prices, categories
  - `UserRepository` - 3 test users (alice, bob, admin)
  - `CartRepository` - User shopping carts (userId → plantId → CartItem)
- `com.example.webflux.service` - Business logic services (3 services + Telegram DTOs)
  - `PlantService` - Plant catalog operations
  - `UserService` - User CRUD operations
  - `CartService` - Cart operations + localStorage merge logic
  - `telegram/dto/` - 24 Telegram Bot API DTOs (not currently used, prepared for future bot integration)
- `com.example.webflux.model` - Domain models
- `com.example.webflux.controller.model` - DTOs for API requests/responses (11 DTOs)

**Key Patterns**:
- Controllers use `suspend fun` for async endpoints
- Streaming endpoints return `Flow<T>` (not Flux/Mono)
- OpenAPI/Swagger annotations for API documentation (write in Russian)
- CORS configured globally in WebFluxConfig
- **Public access**: All frontend pages and plant API are publicly accessible. Authentication is optional.

**Code Style**:
- For single-statement blocks (if, for, etc.) in Kotlin and TypeScript, omit curly braces
- Multi-statement blocks (e.g., for loop with multiple operations) must keep curly braces

### Frontend Architecture

**React + TypeScript SPA** using React Router v7 for navigation.

**Key Directories**:
- `src/components/` - React components (9 components)
  - `PlantCatalog.tsx` - Main catalog page with grid layout
  - `PlantCard.tsx` - Individual plant card with image carousel
  - `ImageCarousel.tsx` - Auto-scrolling image carousel (hover-activated)
  - `CartPage.tsx` - Full shopping cart view with checkout
  - `CartIcon.tsx` - Navbar cart icon with badge counter
  - `AddToCartButton.tsx` - Reusable add-to-cart button
  - `Login.tsx` - Email/password login form
  - `Register.tsx` - User registration form
  - App navigation integrated into `App.tsx`
- `src/services/` - API client services using axios (3 services)
  - `plantService.ts` - GET /api/plants, GET /api/plants/:id
  - `authService.ts` - POST /api/auth/login, /register, /logout, GET /api/auth/me
  - `cartService.ts` - Cart API + localStorage operations
- `src/contexts/` - React Context providers (2 contexts)
  - `AuthContext.tsx` - Global auth state (user, token, login/logout)
  - `CartContext.tsx` - Global cart state (cart, add/remove/update, sync)
- `src/types/` - TypeScript type definitions (2 files)
  - `auth.ts` - User, AuthRequest, AuthResponse, RegisterRequest
  - `cart.ts` - CartSummary, CartItem, LocalCartItem, API request types
- `src/utils/` - Utility functions
  - `axiosConfig.ts` - Request/response interceptors (token injection, 401 handling)
- `dist/` - Build output (copied to backend's static resources)

**State Management**: Uses React Context API for global state:
- `AuthContext` - Authentication state (user, token, isAuthenticated, login/logout)
- `CartContext` - Shopping cart state with dual storage:
  - `cart: CartSummary | null` - Server cart for authenticated users
  - `localCartItems: CartItem[]` - Full cart details for anonymous users (loaded from /api/plants)
  - `localCartCount: number` - Quick count from localStorage
  - Functions: add/remove/update, localStorage sync, optimistic updates

**Integration Pattern**: Backend serves frontend static files from `classpath:/static/` after Maven copies `frontend/dist` into `backend/target/classes/static`.

**Design System** (App.css):
- **Color Palette**:
  - `--forest-green: #2d5016` (dark green for headings)
  - `--sage-green: #4a7c2c` (primary green)
  - `--leaf-green: #7fa650` (light green accents)
  - `--warm-brown: #8b7355` (brown for text)
  - `--cream: #f4f1ea` (cream background)
  - `--light-green: #e8f5e0` (light green backgrounds)
  - `--shadow: rgba(45, 80, 22, 0.1)` (shadows)
- **Card Patterns**: White background, `border-radius: 15px`, shadows `0 4px 15px var(--shadow)`
- **Hover Effects**: `translateY(-8px)`, enhanced shadows, green borders
- **Buttons**: Rounded (`border-radius: 25px`), green gradients, smooth transitions
- **Plant Emoji Icons**: Used throughout for visual decoration

**NavBar Design**:
- **Sticky positioning**: `position: sticky`, `top: 0`, `z-index: 1020` - остаётся вверху при скролле
- **Compact height**: `padding: 0.25rem 0` (4px вертикальный padding)
- **Menu items**: Главная, Корзина (с badge счётчиком), Вход/Регистрация (или Привет {имя}/Выход для авторизованных)
- **Logout behavior**: После выхода редирект на главную страницу `/`
- **No API docs links**: Swagger и API документация убраны из навигации
- **Cart Icon**: Интегрирована в навбар с динамическим счётчиком товаров

**ImageCarousel Component**:
- Auto-scrolling activates on mouse hover (2 second intervals)
- Auto-scrolling stops when user manually clicks navigation buttons/indicators
- Auto-scrolling resumes on next mouse enter
- Uses `onSelect` event handler with `event.source !== 'timer'` check to detect manual interactions
- State management: `isHovered` (mouse over) + `isManuallyPaused` (manual click detection)

## API Endpoints

Base URL: `http://localhost:8080`

**API Statistics:**
- **Total endpoints**: 20+ REST endpoints
- **Public endpoints**: 9 (frontend pages + plant catalog + login/register)
- **Protected endpoints**: 11 (cart operations + user management + me/logout)
- **Admin-only endpoints**: 1 (DELETE /api/users/{id})

### Public Endpoints (No Authentication Required)

**Frontend Pages**:
- `GET /` - Main page (React SPA)
- `GET /catalog` - Plant catalog page
- `GET /login` - Login page
- `GET /register` - Registration page
- `GET /assets/**` - Static assets (JS, CSS, images)

**Public API**:
- `GET /api/hello` - Simple greeting with coroutine delay
- `GET /api/hello/{name}` - Personalized greeting
- `GET /api/stream` - Server-sent events stream (1-10 with 1s delays)
- `GET /api/plants` - Get all plants (used by anonymous cart to load plant details)
- `GET /api/plants/{id}` - Get plant by ID
- `POST /api/auth/login` - User login (returns JWT opaque token)
- `POST /api/auth/register` - User registration (returns JWT opaque token)
- `GET /swagger-ui.html` - Swagger UI
- `GET /api-docs` - OpenAPI JSON spec

**IMPORTANT**: `/api/auth/me` and `/api/auth/logout` are **protected endpoints**, NOT public. Only login and register are public under `/api/auth/**`.

### Protected Endpoints (Require Authentication)
- `POST /api/auth/logout` - Logout (invalidates token)
- `GET /api/auth/me` - Get current user info (MUST be authenticated)
- `GET /api/cart` - Get current user's cart with summary
- `POST /api/cart/items` - Add item to cart
- `PUT /api/cart/items/{plantId}` - Update item quantity
- `DELETE /api/cart/items/{plantId}` - Remove item from cart
- `DELETE /api/cart` - Clear entire cart
- `POST /api/cart/merge` - Merge localStorage cart with server cart (on login)
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (Admin only)

## Configuration

- **Backend Port**: 8080 (application.yml)
- **Frontend Dev Port**: 5173 (Vite default)
- **Static Resources**: Backend serves React app from `/` after build
- **CORS**: Configured for credentials and Authorization header (WebFluxConfig)
- **Security**: JWT Opaque Token with 24-hour expiration. **All frontend pages and plant catalog are publicly accessible** - authentication is optional.
- **Password Hashing**: BCrypt with default strength (10 rounds)

## Development Workflow

1. **Full Stack Development**:
   - Run backend: `cd backend && mvn spring-boot:run`
   - Run frontend dev server separately: `cd frontend && npm run dev`
   - Frontend proxies API calls to backend via axios base URL

2. **Production Build**:
   - `mvn clean install` at root builds both modules
   - Maven frontend plugin installs Node/npm and builds React app
   - Backend pom.xml copies frontend/dist → backend static resources
   - Single JAR contains both backend and frontend

## Testing

Backend tests use JUnit 5 with Spring Boot Test:
```bash
cd backend
mvn test
```

## Technology Versions

- Java: 17
- Kotlin: 2.1.0
- Spring Boot: 4.0.3
- Spring Framework: 7.0.5 (included in Spring Boot)
- Spring Security: 7.0.x (included in Spring Boot)
- Jackson: 3.x (included in Spring Boot)
- Node: v20.11.0
- React: 18.2.0
- TypeScript: 5.4.2

## Authentication & Security

### JWT Opaque Token Implementation

The application uses **JWT Opaque Tokens** (not self-contained JWT) for authentication:

- **Token Format**: UUID-based random strings (not standard JWT with payload)
- **Storage**: Server-side in-memory (TokenStorage with ConcurrentHashMap)
- **Expiration**: 24 hours (configurable in application.yml)
- **Validation**: Requires server lookup on every request
- **Revocation**: Easy invalidation on logout/password change

### Security Architecture

**Public Access Model**: The application uses an open access model where all frontend pages and the plant catalog are publicly accessible. Users can browse the site without authentication and optionally register/login for additional features.

**Security Configuration (SecurityConfig.kt)**:
- **Public auth endpoints**: Only `/api/auth/login` and `/api/auth/register` are permitAll
- **Protected auth endpoints**: `/api/auth/me` and `/api/auth/logout` require authentication
- IMPORTANT: Do NOT use `/api/auth/**` wildcard in permitAll - be explicit about which auth endpoints are public

**Components**:
- `OpaqueTokenAuthenticationToken` - Custom authentication principal (authenticated)
- `OpaqueTokenAuthenticationConverter` - Extracts Bearer token from request, creates `UsernamePasswordAuthenticationToken` (unauthenticated)
- `OpaqueTokenAuthenticationManager` - Validates token via AuthenticationService, converts to `OpaqueTokenAuthenticationToken`
- `SecurityUtils` - Helper to get current user from security context

**Authentication Token Flow**:
1. Converter extracts token from `Authorization: Bearer <token>` header
2. Converter creates unauthenticated `UsernamePasswordAuthenticationToken(token, token)`
3. Manager validates token and converts to authenticated `OpaqueTokenAuthenticationToken` with authorities
4. Spring Security stores authenticated token in context for request processing

**Services**:
- `AuthenticationService` (interface) - Login, register, token validation, revocation
- `LocalAuthenticationService` (implementation) - Local auth logic with BCrypt
- `AuthorizationService` (interface) - RBAC role checking
- `LocalAuthorizationService` (implementation) - Simple role-based authorization

**Design for Keycloak Migration**: Service interfaces allow easy swap to KeycloakAuthenticationService in future without changing controllers.

### RBAC (Role-Based Access Control)

**Roles**:
- `USER` - Standard user (default for new registrations)
- `ADMIN` - Administrator with elevated privileges

**Implementation**:
- Roles stored as `Set<String>` in User model
- Spring Security `@PreAuthorize` annotations on controller methods
- Example: `@PreAuthorize("hasRole('ADMIN')")` for admin-only endpoints
- `@PreAuthorize("isAuthenticated()")` for any authenticated user

**Test Users** (password: `password123`):
- alice@example.com - USER role
- bob@example.com - USER role
- admin@example.com - USER + ADMIN roles

### Frontend Authentication

**AuthContext** (React Context API):
- Global authentication state (user, token, isAuthenticated, loading)
- Functions: login(), register(), logout(), hasRole()
- Automatic token injection via Axios interceptors (reads from localStorage directly)
- Token persistence in localStorage
- Session restoration on page reload
- Auto-redirect to /login on 401 responses
- Logout redirects to homepage `/` (not login page)

**CRITICAL: Axios Interceptor Implementation**:
- Interceptor reads token **directly from localStorage** (not from React state)
- This prevents race conditions when CartContext syncs immediately after login
- Token is saved to localStorage BEFORE updating React state to ensure availability
- Only adds Authorization header if token exists (doesn't send empty Bearer headers)

**Protected Routes**: Not implemented yet, but easy to add using `isAuthenticated` from useAuth hook.

**Components**:
- `Login` - Login form with email/password (styled with GreenDecor theme)
- `Register` - Registration form with password confirmation (styled with GreenDecor theme)
- Updated `Navbar` - Shows user name and logout button when authenticated

**UI Design**:
- Login/Register pages styled consistently with main site design
- Centered card with white background, rounded corners (`border-radius: 15px`)
- Hover effects with green border (`var(--leaf-green)`) and enhanced shadows
- Plant emoji icons (🌿 for login, 🪴 for register)
- Custom submit buttons with green gradient and smooth transitions
- Styled links in site color palette (`var(--sage-green)`, `var(--forest-green)`)
- Responsive design for mobile devices

### Security Best Practices

- ✅ BCrypt password hashing (never store plain passwords)
- ✅ CORS properly configured with credentials
- ✅ CSRF disabled for stateless API (safe with opaque tokens)
- ✅ HTTP-only cookies NOT used (token in localStorage for SPA)
- ✅ Token expiration enforced server-side
- ✅ Authorization header format: `Bearer <token>`
- ⚠️ HTTPS not configured (use reverse proxy in production)
- ⚠️ Refresh tokens not implemented (planned for future)

### Authentication Flow

1. **Registration**: POST /api/auth/register → User created → Token generated → Token + User returned
2. **Login**: POST /api/auth/login → Credentials validated → Token generated → Token + User returned
3. **Authenticated Request**: Request + `Authorization: Bearer <token>` header → Token validated → User context set → Endpoint executed
4. **Logout**: POST /api/auth/logout → Token removed from TokenStorage → Client clears localStorage

## Shopping Cart System

### Dual Cart Architecture

The application implements a sophisticated cart system that works for both authenticated and anonymous users:

**Two-tier cart storage:**
1. **localStorage cart** - For anonymous users (client-side only)
2. **Server-side cart** - For authenticated users (persisted in CartRepository)

**Key Features:**
- Anonymous users can add items to cart (stored in localStorage with key `localCart`)
- **Anonymous users can view full cart details** - CartContext loads plant details via public `/api/plants` API
- Cart persists across page refreshes for anonymous users
- On login: localStorage cart automatically merges with server cart
- On logout: server cart clears, localStorage cart restored
- **True optimistic UI updates for both authenticated and anonymous users** - No reload after cart operations
- **Smooth UX** - Only modified row and totals update, table doesn't reload
- Toast notifications for all cart operations
- **Unified CartPage UI** - Same cart display for authenticated and anonymous users

### Cart Display for Anonymous Users (Added: 2026-03-19)

**Problem Solved**: Previously, anonymous users could add items to cart but CartPage only showed item count without details. They had to login just to see what was in their cart.

**CartContext.tsx - localCartItems state:**
- `localCartItems: CartItem[]` - Full plant details for localStorage cart
- `loadLocalCartItems()` function (lines ~100-135):
  1. Reads `localCart` from localStorage (plantId + quantity only)
  2. Fetches all plants via public `GET /api/plants` API
  3. Creates Map<plantId, Plant> for fast lookup
  4. Matches localStorage plantIds with plant data
  5. Filters out deleted plants (returns null, filtered out)
  6. Builds `CartItem[]` with full details (images, prices, names)
  7. Handles errors gracefully with toast notification
- Called automatically when user is not authenticated
- Re-loads after every cart operation (add, update, remove, clear)
- Exported in CartContextType interface and provider value

**CartPage.tsx - Unified display logic:**
- Removed early return that blocked anonymous users (old lines 18-42)
- Added `displayCart` variable that selects appropriate cart source:
  - **Authenticated**: server `cart` from API (CartSummary)
  - **Anonymous**: builds CartSummary from `localCartItems` with calculated totals
- Same rich UI for both user types:
  - Table with plant images, names, categories, prices
  - Quantity controls (+/- buttons)
  - Remove button per item
  - Total items count and total price
- Checkout button changes based on auth state:
  - **Authenticated**: "Оформить заказ ✓" (placeholder functionality)
  - **Anonymous**: "Войдите для оформления заказа 🌿" (redirects to /login)
- All cart operations work for anonymous users (update quantity, remove, clear)

**Implementation Details:**
- `loadLocalCartItems()` uses `plantService.getAllPlants()` - acceptable for 8 plants (scales to ~100)
- Plant details refresh on page load, not on every cart operation
- Uses Map for O(1) lookup when matching plantIds
- Error handling: shows toast and empty cart if API fails
- **True optimistic updates (2026-03-20):**
  - **Authenticated users**: Cart operations apply changes to `cart` state immediately
  - **Anonymous users**: Cart operations apply changes to `localCartItems` state immediately
  - Server request or localStorage update happens in background
  - On error: rollback to previous state (`previousCart` or `previousLocalItems` snapshot)
  - Result: Only changed row and totals update, no table flicker for both user types

**Performance Considerations:**
- `GET /api/plants` called once per cart page load (not per item)
- For large catalogs (>100 plants), consider caching plant data or batch endpoint
- Current implementation optimal for small-to-medium catalogs

### Cart Synchronization Flow (Login)

**CartContext.tsx critical logic:**

```typescript
// When user logs in with items in localStorage:
1. AuthContext.login() saves token to localStorage first (prevents race condition)
2. Sets user/token in React state → triggers isAuthenticated change
3. CartContext useEffect detects auth state change
4. Calls syncAndLoadCart()
5. Reads localCart from localStorage
6. If items exist: POST /api/cart/merge with localItems
7. Clears localStorage after successful merge
8. Shows success toast: "🔄 Корзина синхронизирована!"
```

**Merge Strategy (CartRepository.kt:87-113):**
- If item exists in both carts: quantities are **summed** (not replaced)
- If item only in localStorage: added to server cart
- Server cart becomes source of truth after merge

### Optimistic UI Updates (Improved: 2026-03-20)

**CartContext implements true optimistic updates for both authenticated and anonymous users:**

**Authenticated users (server cart):**
- `addToCart()` - Immediately updates quantity in `cart` state, no server reload for existing items
- `updateQuantity()` - Instantly updates `cart` state, **no loadServerCart() call**
- `removeItem()` - Immediately removes from `cart` state, **no loadServerCart() call**
- `clearCart()` - Instantly clears `cart` state, **no loadServerCart() call**
- Saves `previousCart` snapshot for rollback on error

**Anonymous users (localStorage cart):**
- `addToCart()` - Immediately updates quantity in `localCartItems` state, no plant fetch for existing items
- `updateQuantity()` - Instantly updates `localCartItems` state, **no loadLocalCartItems() call**
- `removeItem()` - Immediately removes from `localCartItems` state, **no loadLocalCartItems() call**
- `clearCart()` - Instantly clears `localCartItems` state, **no loadLocalCartItems() call**
- Saves `previousLocalItems` snapshot for rollback on error

**Benefits:**
- Zero perceived latency for user actions (both authenticated and anonymous)
- **No table flicker** - only modified row and totals update
- Graceful error handling with rollback to previous state
- Server/localStorage validates in background but doesn't override optimistic state

**Technical Implementation:**
- Each operation captures `const previousCart = cart` or `const previousLocalItems = localCartItems`
- Applies optimistic state update via `setCart(optimisticCart)` or `setLocalCartItems(updatedItems)`
- Sends server request or localStorage update without awaiting reload
- On error: restores previous state with toast notification
- `loadServerCart()` and `loadLocalCartItems()` removed from success path

### Cart Components

**Frontend Components:**
- `CartPage.tsx` - Full cart view with item list, quantities, totals, checkout (works for both authenticated and anonymous users)
- `CartIcon.tsx` - Navbar icon with badge showing total items count
- `AddToCartButton.tsx` - Reusable button for adding items to cart
- `PlantCard.tsx` - Displays plant with integrated AddToCartButton

**Backend Components:**
- `CartController.kt` - REST API for cart operations (all @PreAuthorize protected)
- `CartService.kt` - Business logic (merge, summary calculation, validation)
- `CartRepository.kt` - In-memory storage with ConcurrentHashMap

### Cart Data Models

**Backend (Kotlin):**
- `CartItem` (model) - id, userId, plantId, quantity, addedAt
- `CartItemDto` (API response) - Full plant details + quantity + addedAt
- `CartSummaryDto` - items[], totalItems, totalPrice
- `LocalCartItem` (merge request) - plantId, quantity (from localStorage)
- `AddToCartRequest` - plantId, quantity
- `UpdateQuantityRequest` - quantity
- `MergeCartRequest` - items: LocalCartItem[]

**Frontend (TypeScript):**
- `CartSummary` - Matches backend CartSummaryDto
- `CartItem` - Matches backend CartItemDto
- `LocalCartItem` - For localStorage storage (plantId + quantity only)

### Troubleshooting Cart Issues

**localStorage cart not syncing on login:**
- Check browser DevTools → Application → LocalStorage → `localCart` key exists
- Verify Network tab shows POST `/api/cart/merge` with Authorization header
- Check console for errors during syncAndLoadCart()

**Race condition errors (HTTP Basic Auth dialog on login):**
- Fixed: Axios interceptor now reads token from localStorage directly (not React state)
- Token saved to localStorage BEFORE state update to prevent timing issues
- See axiosConfig.ts and AuthContext.tsx:48-57 for implementation

**Cart count not updating:**
- Check CartContext.getTotalItems() logic
- Verify isAuthenticated state is correct
- For authenticated: uses cart.totalItems from server
- For anonymous: uses localCartCount from localStorage

**Anonymous users can't see cart items (pre-2026-03-19 issue):**
- Fixed: CartContext now loads full plant details via `loadLocalCartItems()`
- CartPage renders unified view using `displayCart` for both user types
- localStorage cart now displays same rich UI as server cart

**Empty cart showing for anonymous users with items:**
- Verify `loadLocalCartItems()` is being called in useEffect
- Check browser console for errors during plant details loading
- Ensure `/api/plants` is accessible (public endpoint)

### Troubleshooting Authentication

**401 Unauthorized on protected endpoint**:
- Check token is in request: `Authorization: Bearer <token>`
- Verify token hasn't expired (check TokenStorage)
- Ensure endpoint isn't in permitAll list by mistake

**Token not being sent from frontend**:
- Check Axios interceptor is configured (axiosConfig.ts)
- Verify token exists in localStorage
- Check browser DevTools Network tab for Authorization header

**Jackson deserialization errors**:
- Ensure `@JsonProperty` annotations on Kotlin data classes
- Verify JacksonConfiguration is configuring kotlinModule()
- Spring Boot 3.2.2 uses Jackson 2.x (not 3.x like Spring Boot 4.x)

**500 Error: "No provider found for class PreAuthenticationToken" (Fixed: 2026-03-20)**:
- Problem: `AuthenticationWebFilter` couldn't find manager for custom `PreAuthenticationToken`
- Root cause: Spring Security WebFlux requires explicit manager registration via `DelegatingReactiveAuthenticationManager`
- Solution: Wrap `OpaqueTokenAuthenticationManager` in `DelegatingReactiveAuthenticationManager`
- Files modified: `SecurityConfig.kt` (line 28-31)
- Pattern: Always use `DelegatingReactiveAuthenticationManager` when using custom authentication tokens in WebFlux

### Future Enhancements

**Planned for Keycloak Migration**:
1. Replace LocalAuthenticationService with KeycloakAuthenticationService
2. Update SecurityConfig to use Keycloak JWT validation
3. Configure Keycloak realm and client
4. No changes needed to controllers (abstracted via interfaces)

**Possible Improvements**:
- Refresh tokens for better UX
- Password reset flow via email
- Account activation via email
- Rate limiting on login attempts
- Session management (view active sessions, revoke all)
- JWT standard tokens (self-contained) for stateless scaling

## Known Fixed Issues

### HTTP Basic Auth Dialog on Login with Cart Items (Fixed: 2026-03-19)

**Problem**: When user tried to login with items in localStorage cart, browser showed HTTP Basic Authentication dialog instead of completing login.

**Root Cause**: Race condition between React state updates and Axios interceptor setup:
- `AuthContext.login()` set token in React state (async)
- `CartContext` detected `isAuthenticated` change and called `syncAndLoadCart()` immediately
- Axios interceptor closure still had old `token` value (null)
- Request to `/api/cart/merge` sent without Authorization header
- Backend returned 401 → Spring Security showed Basic Auth prompt

**Solution**:
1. Changed `axiosConfig.ts` - Interceptor now reads token from `localStorage.getItem('token')` directly instead of React state closure
2. Changed `AuthContext.tsx` - Token saved to localStorage BEFORE updating React state
3. This ensures token is always available when CartContext starts synchronization

**Files Modified**:
- `frontend/src/utils/axiosConfig.ts:6` - Changed from `getToken()` to `localStorage.getItem('token')`
- `frontend/src/contexts/AuthContext.tsx:25-27` - Removed token dependency from useEffect
- `frontend/src/contexts/AuthContext.tsx:48-57` - Reordered: localStorage first, then setState

**Testing**: Login with items in cart now works correctly. Cart merge happens seamlessly without auth dialogs.

### 500 Server Error for /api/auth/me (Fixed: 2026-03-19)

**Problem**: When anonymous users visited the site, backend logs showed:
```
java.lang.IllegalStateException: No provider found for class UsernamePasswordAuthenticationToken
```

**Root Cause**: SecurityConfig had `/api/auth/**` in permitAll list, which included `/api/auth/me`. This endpoint should require authentication, but the wildcard made it public. When frontend sent invalid/expired tokens, Spring Security tried to authenticate but couldn't find proper authentication provider.

**Solution**:
1. Changed `SecurityConfig.kt` - Split `/api/auth/**` into specific paths:
   - **Public**: `/api/auth/login`, `/api/auth/register` (permitAll)
   - **Protected**: `/api/auth/me`, `/api/auth/logout` (authenticated)
2. Changed `AuthContext.tsx` - Added explicit state reset (`setUser(null)`, `setToken(null)`) when token validation fails

**Files Modified**:
- `backend/src/main/kotlin/com/example/webflux/config/SecurityConfig.kt:41` - Changed from wildcard to specific paths
- `frontend/src/contexts/AuthContext.tsx:39-42` - Added explicit null state on validation error

**Testing**: Anonymous users no longer trigger 500 errors. Invalid tokens are handled gracefully with proper 401 responses.

### Table Flicker on Cart Updates (Fixed: 2026-03-20)

**Problem**: When user changed quantity or removed items in cart, entire table reloaded from server causing visible flicker. All rows disappeared and reappeared even though only one row changed.

**Root Cause**: After applying optimistic updates, CartContext called `loadServerCart()` which replaced entire cart state, forcing React to re-render all table rows.

**Solution**:
1. Removed `loadServerCart()` calls from success path in cart operations
2. Server request still validates in background but doesn't reload state
3. Optimistic update becomes the source of truth unless error occurs
4. On error: rollback to `previousCart` snapshot with toast notification

**Files Modified**:
- `frontend/src/contexts/CartContext.tsx:182-235` - updateQuantity: removed loadServerCart() call, added localStorage optimistic updates
- `frontend/src/contexts/CartContext.tsx:237-275` - removeItem: removed loadServerCart() call, added localStorage optimistic updates
- `frontend/src/contexts/CartContext.tsx:142-180` - addToCart: only reload if adding new item, added localStorage optimistic updates
- `frontend/src/contexts/CartContext.tsx:277-284` - clearCart: removed loadServerCart() call, added localStorage optimistic updates

**Testing**: Cart updates are now instant with zero flicker for both authenticated and anonymous users. Only modified row and totals update.

### Cart Badge Not Showing on Page Load (Fixed: 2026-03-20)

**Problem**: When user loaded the site or refreshed the page, the cart badge (red circle with item count) didn't appear in navbar, even though items were in localStorage.

**Root Cause**: `CartContext` initialized `localCartCount` as 0 but never loaded the actual count from localStorage on mount. The count was only updated after cart operations (add/remove).

**Solution**:
1. Added `useEffect` hook that runs once on component mount
2. Calls `updateLocalCartCount()` to read from localStorage
3. Only runs for unauthenticated users (authenticated users load cart from server)

**Files Modified**:
- `frontend/src/contexts/CartContext.tsx:32-35` - Added initialization useEffect to load localCartCount on mount

**Testing**: Refresh page with items in cart → badge appears immediately with correct count.

### 401 Error on Cart Page Refresh for Authenticated Users (Fixed: 2026-03-20)

**Problem**: When logged-in user refreshed cart page (F5), backend returned 401 error and cart didn't load.

**Root Cause**: Race condition between AuthContext and CartPage:
1. `CartPage` mounted and called `refreshCart()` in useEffect with empty deps `[]`
2. At this moment `AuthContext` was still validating token from localStorage (`loading: true`)
3. `isAuthenticated` was temporarily `false` even though valid token existed
4. `refreshCart()` didn't call `loadServerCart()` because user appeared unauthenticated
5. Or if `refreshCart()` tried to call server, token wasn't validated yet → 401

**Solution**:
1. `CartPage` now reads `loading` state from `AuthContext`
2. useEffect waits for `!authLoading` before calling `refreshCart()`
3. This ensures authentication check completes before loading cart
4. Added `authLoading` to useEffect dependencies to trigger refresh when auth completes

**Files Modified**:
- `frontend/src/components/CartPage.tsx:10-18` - Added authLoading check before refreshCart()

**Testing**: Login, navigate to cart, refresh page (F5) → cart loads without 401 errors.

**Additional Fixes**:
1. Changed token validation strategy - instead of calling `/api/auth/me` on page load, user data is restored from localStorage. Token validation happens on first API request (if token expired, 401 interceptor redirects to /login).
2. **Simplified auth logic** - Frontend always sends Authorization header if token exists, backend decides whether to authenticate. `OpaqueTokenAuthenticationConverter` skips public paths, returning `Mono.empty()` to prevent authentication attempt.

## Testing Cart Functionality

### Anonymous User Cart Flow

**Test Scenario 1 - Add and View Items:**
1. Open `http://localhost:8080` without logging in
2. Navigate to catalog
3. Add 2-3 plants to cart with different quantities
4. Click cart icon in navbar → should see full cart page with:
   - Plant images (60x60px thumbnails)
   - Plant names and categories
   - Individual prices and quantities
   - +/- buttons to adjust quantities
   - Remove button (✕) for each item
   - Total items count and total price
   - "Войдите для оформления заказа 🌿" button

**Test Scenario 2 - Cart Persistence:**
1. Add items to cart without logging in
2. Refresh page (F5)
3. Navigate to /cart → items should still be visible with full details

**Test Scenario 3 - Quantity Management:**
1. In cart page (anonymous), click + to increase quantity
2. Click - to decrease quantity
3. Click ✕ to remove item
4. All operations should work instantly with optimistic updates
5. Cart totals should update correctly

**Test Scenario 4 - Clear Cart:**
1. Click "Очистить корзину" button
2. Confirm in dialog
3. Cart should become empty with "Корзина пуста" message

**Test Scenario 5 - Login with Cart Items:**
1. Add 2-3 items to cart (anonymous)
2. Login with alice@example.com / password123
3. Should see toast: "🔄 Корзина синхронизирована! Добавлено X товаров"
4. Cart should show merged items
5. localStorage should be cleared (check DevTools → Application → LocalStorage)

**Test Scenario 6 - Logout:**
1. While logged in with items in cart
2. Click "Выход" in navbar
3. Should redirect to `/` (homepage)
4. Server cart cleared, localStorage cart restored (if any)

**Test Scenario 7 - Smooth Cart Updates (Added: 2026-03-20):**
1. Login and add 3-4 items to cart
2. Navigate to cart page
3. Click + or - to change quantity
4. **Expected**: Only the modified row and total price update instantly, no table flicker
5. Click ✕ to remove item
6. **Expected**: Row disappears instantly without reloading other rows
7. Click "Очистить корзину"
8. **Expected**: Cart becomes empty instantly without loading spinner
