# Дизайн: Rate limiting эндпоинтов аутентификации (реактивный bucket4j WebFilter)

Дата: 2026-06-26
Статус: согласован пользователем (ожидает ревью спеки)

## Цель

Ограничить частоту запросов (rate limit) на три эндпоинта аутентификации,
**отдельным лимитом на каждый**:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`

Реализация — на библиотеке **bucket4j** (token-bucket), интегрированная в
реактивный стек через кастомный `WebFilter`. Хранилище бакетов — **Caffeine**
кэш с автоматическим вытеснением неактивных записей.

## Согласованные решения

| Решение | Выбор |
|---|---|
| Ключ бакета | По IP-адресу клиента: `"<endpoint>:<clientIp>"` |
| Лимиты по умолчанию | Строгие: login 5/мин, register 3/час, logout 10/мин |
| Конфигурация | Через `application.yml` (`@ConfigurationProperties`) |
| Источник IP | `X-Forwarded-For` (первый), иначе `remoteAddress` |
| Очистка памяти | Caffeine `expireAfterAccess` (без `@Scheduled`) |
| Хранилище | In-memory (один инстанс приложения) |
| Стратегия пополнения | Greedy refill (плавное пополнение токенов) |

## Архитектура

### 1. Зависимости (`backend/pom.xml`)

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.19.0</version>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <!-- версия управляется spring-boot-dependencies BOM -->
</dependency>
```

Модуль `bucket4j_jdk17-core` (Java 17+) совместим с Java 21 проекта.
Используются in-memory локальные бакеты (`io.github.bucket4j.Bucket`).
Операция `tryConsumeAndReturnRemaining` — неблокирующая (CAS, без I/O),
поэтому корректно вызывается напрямую в реактивной цепочке фильтра.
Caffeine версионируется Spring Boot BOM — явная версия не нужна.

### 2. Properties (`config/RateLimitProperties.kt`)

По образцу `AiAgentProperties`. Биндинг из секции `rate-limit`, регистрация
через `@EnableConfigurationProperties(RateLimitProperties::class)` (на классе
фильтра или отдельной `@Configuration`).

```kotlin
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    /**
     * Время простоя, после которого бакет вытесняется из Caffeine-кэша
     * (expireAfterAccess). ДОЛЖНО быть >= максимального refill-period среди
     * эндпоинтов, иначе клиент сможет сбросить лимит, переждав TTL.
     */
    val idleTimeout: Duration = Duration.ofHours(1),
    val endpoints: Map<String, EndpointLimit> = emptyMap()
) {
    data class EndpointLimit(
        val capacity: Long,
        val refillPeriod: Duration
    )
}
```

Ключи `endpoints`: `login`, `register`, `logout`.

### 3. application.yml

```yaml
rate-limit:
  enabled: true
  idle-timeout: 1h          # >= максимального refill-period (register = 1h)
  endpoints:
    login:    { capacity: 5,  refill-period: 1m }
    register: { capacity: 3,  refill-period: 1h }
    logout:   { capacity: 10, refill-period: 1m }
```

`Duration` биндится из строк `1m`/`1h` (Spring relaxed binding для `java.time.Duration`).

### 4. WebFilter (`config/AuthRateLimitWebFilter.kt`)

- Реализует `org.springframework.web.server.WebFilter`.
- Бин с `@Order(Ordered.HIGHEST_PRECEDENCE)` — выполняется **до** Spring
  Security (login/register всё равно `permitAll`; брутфорс отсекается до bcrypt;
  logout идентифицируется по IP, контекст безопасности не требуется).
- **Матчинг цели**: маппинг `(HttpMethod.POST, path) -> endpointKey`:
  - `POST /api/auth/login`    → `login`
  - `POST /api/auth/register` → `register`
  - `POST /api/auth/logout`   → `logout`
  - Любой другой запрос → `chain.filter(exchange)` без изменений.
- **`enabled = false`** → фильтр сразу пропускает все запросы.
- **Извлечение IP**: helper `resolveClientIp(request)`:
  - читает заголовок `X-Forwarded-For`, берёт первый непустой токен (до запятой), trim;
  - иначе `request.remoteAddress?.address?.hostAddress`;
  - fallback `"unknown"`.
- **Хранилище бакетов**: Caffeine `Cache<String, Bucket>`:

  ```kotlin
  private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
      .expireAfterAccess(properties.idleTimeout)
      .build()
  ```

  Ключ `"<endpointKey>:<clientIp>"`, получение/создание через
  `buckets.get(key) { buildBucket(endpointKey) }`. Конфигурация бакета строится
  из `EndpointLimit` соответствующего эндпоинта:

  ```kotlin
  Bucket.builder()
      .addLimit { limit ->
          limit.capacity(cfg.capacity)
               .refillGreedy(cfg.capacity, cfg.refillPeriod)
      }
      .build()
  ```

  Любой `get` (попытка списания) сбрасывает таймер простоя, поэтому активные
  клиенты сохраняют бакет, а неактивные вытесняются Caffeine автоматически.
  Вытесненный бакет при следующем запросе создаётся заново (полный) — корректно,
  т.к. за `idleTimeout >= refillPeriod` бакет в любом случае полностью
  восстановился бы.

- **Решение по запросу**:
  ```kotlin
  val probe = bucket.tryConsumeAndReturnRemaining(1)
  if (probe.isConsumed) {
      response.headers.add("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
      chain.filter(exchange)
  } else {
      val retryAfterSeconds = ceil(probe.nanosToWaitForRefill / 1e9).toLong()
      // 429 + Retry-After + JSON body
  }
  ```
- **Ответ при отказе (429)**:
  - статус `HttpStatus.TOO_MANY_REQUESTS`;
  - заголовок `Retry-After: <secs>`;
  - `Content-Type: application/json`;
  - тело: `{"error":"Too Many Requests","retryAfterSeconds":<N>}`, записывается
    через `response.bufferFactory().wrap(bytes)` + `response.writeWith(Mono.just(buffer))`.

## Поток данных

```
Request POST /api/auth/login
   │
   ▼
AuthRateLimitWebFilter (Ordered.HIGHEST_PRECEDENCE)
   │  enabled? ── нет ──► chain.filter
   │  цель? ──── нет ──► chain.filter
   │  да
   ▼
resolveClientIp(X-Forwarded-For | remoteAddress)
   │
   ▼
buckets.get("login:<ip>") { build(login cfg) }   // Caffeine, expireAfterAccess
   │
   ▼
bucket.tryConsumeAndReturnRemaining(1)
   │── consumed ──► +X-Rate-Limit-Remaining ──► chain.filter ──► Spring Security ──► AuthController
   └── отказ ─────► 429 + Retry-After + JSON body (Mono<Void>)
```

## Обработка ошибок

- Невозможность определить IP → ключ с `"unknown"` (общий бакет для таких
  запросов; приемлемо как fallback).
- Отсутствие конфигурации эндпоинта в `endpoints` → запрос пропускается без
  лимита (фильтр логирует WARN один раз). Гарантируем наличие всех трёх ключей в
  `application.yml`.
- Любые иные пути/методы не затрагиваются.

## Тестирование (`AuthRateLimitWebFilterTest.kt`)

Юнит-тесты фильтра (мок `WebFilterChain`, конструирование `MockServerWebExchange`):

1. Первые `capacity` запросов на `login` с одного IP проходят; `capacity+1` → 429.
2. Ответ 429 содержит заголовок `Retry-After` и JSON-тело с `retryAfterSeconds`.
3. Разные IP не делят бакет (изоляция по ключу).
4. `register` и `logout` имеют независимые счётчики от `login` и друг от друга.
5. `enabled = false` → лимит не применяется (любое число запросов проходит).
6. Не-целевой путь/метод (`GET /api/auth/login`, `POST /api/goods`) не затрагивается.
7. `X-Forwarded-For` имеет приоритет над `remoteAddress` при выборе IP.

## Затрагиваемые файлы

| Файл | Действие |
|---|---|
| `backend/pom.xml` | + зависимости bucket4j, caffeine |
| `backend/src/main/kotlin/com/example/webflux/config/RateLimitProperties.kt` | новый |
| `backend/src/main/kotlin/com/example/webflux/config/AuthRateLimitWebFilter.kt` | новый |
| `backend/src/main/resources/application.yml` | + секция `rate-limit` |
| `backend/src/test/kotlin/com/example/webflux/config/AuthRateLimitWebFilterTest.kt` | новый |

## Вне рамок (YAGNI)

- Распределённое хранилище (Redis/Hazelcast) — приложение однонодовое.
- Лимиты на прочие эндпоинты — только три эндпоинта auth.
- Динамическая перезагрузка лимитов в рантайме — меняются через рестарт.
- Whitelist/blacklist IP.
