# Auth Rate Limiting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ограничить частоту запросов на `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout` отдельным лимитом на каждый эндпоинт, через реактивный `WebFilter` на bucket4j с хранилищем бакетов в Caffeine-кэше.

**Architecture:** Кастомный `WebFilter` (`@Order(HIGHEST_PRECEDENCE)`, до Spring Security) перехватывает три целевых POST-запроса, идентифицирует клиента по IP (`X-Forwarded-For` → `remoteAddress`), берёт/создаёт per-(endpoint+IP) `Bucket` из Caffeine-кэша (`expireAfterAccess`), пытается списать токен; при отказе возвращает `429` с `Retry-After`. Лимиты конфигурируются через `@ConfigurationProperties(prefix="rate-limit")` в `application.yml`.

**Tech Stack:** Kotlin 2.1.20, Spring Boot 4.0.3 WebFlux, bucket4j `bucket4j_jdk17-core` 8.19.0, Caffeine (версия из Spring Boot BOM), JUnit 5 + AssertJ + reactor-test.

## Global Constraints

- Java 21 / `jvmTarget=21`; Kotlin 2.1.20; Spring Boot 4.0.3 (версии менять нельзя).
- bucket4j: `com.bucket4j:bucket4j_jdk17-core:8.19.0` (Java-пакет остаётся `io.github.bucket4j`).
- Caffeine: `com.github.ben-manes.caffeine:caffeine` — **без** явной версии (управляется spring-boot-dependencies BOM).
- Приложение однонодовое — только in-memory хранилище, без Redis/распределённых бэкендов.
- Изменять только три эндпоинта auth; прочие пути/методы фильтр не затрагивает.
- Тесты: JUnit 5 (`org.junit.jupiter.api.Test`), пакет `com.example.webflux.config`.
- Лимиты по умолчанию (строгие): login 5/мин, register 3/час, logout 10/мин.
- `idle-timeout` (Caffeine `expireAfterAccess`) ≥ максимального `refill-period` (по умолчанию `1h`).
- Стратегия пополнения — greedy refill.
- Команды Maven запускать из каталога `backend`.

---

### Task 1: Добавить зависимости bucket4j и Caffeine

**Files:**
- Modify: `backend/pom.xml` (блок `<dependencies>`, после guava ~ строка 147)

**Interfaces:**
- Consumes: ничего.
- Produces: на classpath доступны `io.github.bucket4j.Bucket`, `io.github.bucket4j.ConsumptionProbe`, `com.github.benmanes.caffeine.cache.Caffeine`, `com.github.benmanes.caffeine.cache.Cache`.

- [ ] **Step 1: Добавить зависимости в `backend/pom.xml`**

Вставить сразу после закрывающего `</dependency>` блока guava (перед комментарием `<!-- Test -->`):

```xml
        <!-- Rate limiting: bucket4j (token-bucket) -->
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j_jdk17-core</artifactId>
            <version>8.19.0</version>
        </dependency>

        <!-- Caffeine cache для хранения бакетов (версия из Spring Boot BOM) -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
```

- [ ] **Step 2: Проверить, что зависимости резолвятся и проект компилируется**

Run: `cd backend && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS, без ошибок «could not resolve dependency» для bucket4j/caffeine.

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "build: add bucket4j and caffeine for auth rate limiting"
```

---

### Task 2: RateLimitProperties (конфигурация лимитов)

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/config/RateLimitProperties.kt`
- Test: `backend/src/test/kotlin/com/example/webflux/config/RateLimitPropertiesTest.kt`

**Interfaces:**
- Consumes: ничего.
- Produces:
  - `RateLimitProperties(enabled: Boolean, idleTimeout: Duration, endpoints: Map<String, EndpointLimit>)`
  - вложенный `RateLimitProperties.EndpointLimit(capacity: Long, refillPeriod: Duration)`
  - аннотирован `@ConfigurationProperties(prefix = "rate-limit")`.

- [ ] **Step 1: Написать падающий binding-тест**

Создать `backend/src/test/kotlin/com/example/webflux/config/RateLimitPropertiesTest.kt`:

```kotlin
package com.example.webflux.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration

class RateLimitPropertiesTest {

    @EnableConfigurationProperties(RateLimitProperties::class)
    class TestConfig

    private val runner = ApplicationContextRunner()
        .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration::class.java))
        .withUserConfiguration(TestConfig::class.java)

    @Test
    fun `binds enabled, idleTimeout and per-endpoint limits`() {
        runner.withPropertyValues(
            "rate-limit.enabled=true",
            "rate-limit.idle-timeout=1h",
            "rate-limit.endpoints.login.capacity=5",
            "rate-limit.endpoints.login.refill-period=1m",
            "rate-limit.endpoints.register.capacity=3",
            "rate-limit.endpoints.register.refill-period=1h",
            "rate-limit.endpoints.logout.capacity=10",
            "rate-limit.endpoints.logout.refill-period=1m",
        ).run { ctx ->
            val props = ctx.getBean(RateLimitProperties::class.java)
            assertThat(props.enabled).isTrue()
            assertThat(props.idleTimeout).isEqualTo(Duration.ofHours(1))
            assertThat(props.endpoints["login"]!!.capacity).isEqualTo(5L)
            assertThat(props.endpoints["login"]!!.refillPeriod).isEqualTo(Duration.ofMinutes(1))
            assertThat(props.endpoints["register"]!!.refillPeriod).isEqualTo(Duration.ofHours(1))
            assertThat(props.endpoints["logout"]!!.capacity).isEqualTo(10L)
        }
    }

    @Test
    fun `applies defaults when not specified`() {
        runner.run { ctx ->
            val props = ctx.getBean(RateLimitProperties::class.java)
            assertThat(props.enabled).isTrue()
            assertThat(props.idleTimeout).isEqualTo(Duration.ofHours(1))
            assertThat(props.endpoints).isEmpty()
        }
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что он падает (нет класса)**

Run: `cd backend && mvn -q test -Dtest=RateLimitPropertiesTest`
Expected: FAIL — ошибка компиляции «unresolved reference: RateLimitProperties».

- [ ] **Step 3: Создать `RateLimitProperties.kt`**

Создать `backend/src/main/kotlin/com/example/webflux/config/RateLimitProperties.kt`:

```kotlin
package com.example.webflux.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Конфигурация rate limiting для эндпоинтов аутентификации.
 *
 * Биндится из секции `rate-limit` в application.yml.
 * Лимит задаётся отдельно на каждый эндпоинт (ключи: login, register, logout).
 */
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    /** Глобальный выключатель: при false фильтр пропускает все запросы. */
    val enabled: Boolean = true,

    /**
     * Время простоя, после которого бакет вытесняется из Caffeine-кэша
     * (expireAfterAccess). ДОЛЖНО быть >= максимального refillPeriod среди
     * эндпоинтов, иначе клиент сможет сбросить лимит, переждав TTL.
     */
    val idleTimeout: Duration = Duration.ofHours(1),

    /** Лимиты по ключу эндпоинта: login / register / logout. */
    val endpoints: Map<String, EndpointLimit> = emptyMap()
) {
    data class EndpointLimit(
        /** Максимум токенов (число запросов в окне). */
        val capacity: Long,
        /** Период, за который пополняется capacity токенов (greedy refill). */
        val refillPeriod: Duration
    )
}
```

- [ ] **Step 4: Запустить тест — убедиться, что он проходит**

Run: `cd backend && mvn -q test -Dtest=RateLimitPropertiesTest`
Expected: PASS (2 теста).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/com/example/webflux/config/RateLimitProperties.kt backend/src/test/kotlin/com/example/webflux/config/RateLimitPropertiesTest.kt
git commit -m "feat: add RateLimitProperties config binding"
```

---

### Task 3: AuthRateLimitWebFilter (фильтр + регистрация properties)

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/config/AuthRateLimitWebFilter.kt`
- Test: `backend/src/test/kotlin/com/example/webflux/config/AuthRateLimitWebFilterTest.kt`

**Interfaces:**
- Consumes: `RateLimitProperties` и `RateLimitProperties.EndpointLimit` (из Task 2).
- Produces:
  - `class AuthRateLimitWebFilter(properties: RateLimitProperties) : org.springframework.web.server.WebFilter`
  - бин (`@Component`), `@Order(Ordered.HIGHEST_PRECEDENCE)`, регистрирует `RateLimitProperties` через `@EnableConfigurationProperties`.
  - при превышении лимита: статус `429`, заголовок `Retry-After` (секунды), тело `{"error":"Too Many Requests","retryAfterSeconds":<N>}`; при успехе — заголовок `X-Rate-Limit-Remaining`.

- [ ] **Step 1: Написать падающие тесты фильтра**

Создать `backend/src/test/kotlin/com/example/webflux/config/AuthRateLimitWebFilterTest.kt`:

```kotlin
package com.example.webflux.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.time.Duration

class AuthRateLimitWebFilterTest {

    private val strictProps = RateLimitProperties(
        enabled = true,
        idleTimeout = Duration.ofHours(1),
        endpoints = mapOf(
            "login" to RateLimitProperties.EndpointLimit(5, Duration.ofMinutes(1)),
            "register" to RateLimitProperties.EndpointLimit(3, Duration.ofHours(1)),
            "logout" to RateLimitProperties.EndpointLimit(10, Duration.ofMinutes(1)),
        )
    )

    private class CountingChain : WebFilterChain {
        var count = 0
        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            count++
            return Mono.empty()
        }
    }

    private fun callPost(
        filter: AuthRateLimitWebFilter,
        path: String,
        xff: String? = "1.1.1.1",
        remote: String = "9.9.9.9",
    ): ServerWebExchange {
        var builder = MockServerHttpRequest.post(path)
            .remoteAddress(InetSocketAddress(remote, 40000))
        if (xff != null) builder = builder.header("X-Forwarded-For", xff)
        val exchange = MockServerWebExchange.from(builder.build())
        filter.filter(exchange, CountingChain()).block()
        return exchange
    }

    private fun callGet(filter: AuthRateLimitWebFilter, path: String): ServerWebExchange {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build())
        filter.filter(exchange, CountingChain()).block()
        return exchange
    }

    @Test
    fun `allows up to capacity then returns 429 on login`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) {
            val ex = callPost(filter, "/api/auth/login")
            assertThat(ex.response.statusCode).isNull() // прошло дальше по цепочке
        }
        val blocked = callPost(filter, "/api/auth/login")
        assertThat(blocked.response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun `429 response has Retry-After header and json body`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) { callPost(filter, "/api/auth/login") }
        val blocked = callPost(filter, "/api/auth/login")

        assertThat(blocked.response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(blocked.response.headers.getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank()
        val body = (blocked.response as org.springframework.mock.http.server.reactive.MockServerHttpResponse)
            .bodyAsString.block()
        assertThat(body).contains("Too Many Requests").contains("retryAfterSeconds")
    }

    @Test
    fun `different IPs do not share a bucket`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) { callPost(filter, "/api/auth/login", xff = "1.1.1.1") }
        // другой IP — отдельный бакет, проходит
        val other = callPost(filter, "/api/auth/login", xff = "2.2.2.2")
        assertThat(other.response.statusCode).isNull()
    }

    @Test
    fun `endpoints have independent counters`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) { callPost(filter, "/api/auth/login") }       // login исчерпан
        // register всё ещё разрешён (3 токена), logout тоже
        assertThat(callPost(filter, "/api/auth/register").response.statusCode).isNull()
        assertThat(callPost(filter, "/api/auth/logout").response.statusCode).isNull()
        // а login заблокирован
        assertThat(callPost(filter, "/api/auth/login").response.statusCode)
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun `disabled filter never limits`() {
        val filter = AuthRateLimitWebFilter(strictProps.copy(enabled = false))
        repeat(50) {
            assertThat(callPost(filter, "/api/auth/login").response.statusCode).isNull()
        }
    }

    @Test
    fun `non-target requests are untouched`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        // GET на тот же путь — не цель
        repeat(20) {
            assertThat(callGet(filter, "/api/auth/login").response.statusCode).isNull()
        }
        // POST на нецелевой путь — не цель
        repeat(20) {
            assertThat(callPost(filter, "/api/goods").response.statusCode).isNull()
        }
    }

    @Test
    fun `X-Forwarded-For takes priority over remoteAddress`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        // одинаковый remoteAddress, разные XFF — должны быть разные бакеты
        repeat(5) { callPost(filter, "/api/auth/login", xff = "1.1.1.1", remote = "9.9.9.9") }
        val blockedSameXff = callPost(filter, "/api/auth/login", xff = "1.1.1.1", remote = "9.9.9.9")
        assertThat(blockedSameXff.response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        val otherXff = callPost(filter, "/api/auth/login", xff = "2.2.2.2", remote = "9.9.9.9")
        assertThat(otherXff.response.statusCode).isNull()
    }
}
```

- [ ] **Step 2: Запустить тесты — убедиться, что они падают (нет класса фильтра)**

Run: `cd backend && mvn -q test -Dtest=AuthRateLimitWebFilterTest`
Expected: FAIL — «unresolved reference: AuthRateLimitWebFilter».

- [ ] **Step 3: Создать `AuthRateLimitWebFilter.kt`**

Создать `backend/src/main/kotlin/com/example/webflux/config/AuthRateLimitWebFilter.kt`:

```kotlin
package com.example.webflux.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import kotlin.math.ceil

/**
 * Rate limiting WebFilter для эндпоинтов аутентификации.
 *
 * Перехватывает POST /api/auth/{login,register,logout} и ограничивает частоту
 * запросов отдельным лимитом на каждый эндпоинт (bucket4j token-bucket).
 * Клиент идентифицируется по IP (X-Forwarded-For → remoteAddress).
 * Бакеты хранятся в Caffeine-кэше с автоматическим вытеснением неактивных
 * (expireAfterAccess = idleTimeout).
 *
 * @Order(HIGHEST_PRECEDENCE) — выполняется до Spring Security, чтобы отсекать
 * избыточные запросы (в т.ч. брутфорс) до bcrypt-проверки.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(RateLimitProperties::class)
class AuthRateLimitWebFilter(
    private val properties: RateLimitProperties
) : WebFilter {

    private companion object {
        private val log = LoggerFactory.getLogger(AuthRateLimitWebFilter::class.java)

        /** path -> ключ эндпоинта в properties.endpoints */
        private val TARGETS = mapOf(
            "/api/auth/login" to "login",
            "/api/auth/register" to "register",
            "/api/auth/logout" to "logout",
        )
    }

    /** Кэш бакетов: ключ "<endpointKey>:<clientIp>". */
    private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(properties.idleTimeout)
        .build()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!properties.enabled) return chain.filter(exchange)

        val request = exchange.request
        if (request.method != HttpMethod.POST) return chain.filter(exchange)

        val endpointKey = TARGETS[request.uri.path] ?: return chain.filter(exchange)
        val cfg = properties.endpoints[endpointKey]
        if (cfg == null) {
            log.warn("Rate limit not configured for endpoint '{}', passing through", endpointKey)
            return chain.filter(exchange)
        }

        val clientIp = resolveClientIp(request)
        val bucket = buckets.get("$endpointKey:$clientIp") { buildBucket(cfg) }

        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            exchange.response.headers.add("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            return chain.filter(exchange)
        }

        val retryAfterSeconds = ceil(probe.nanosToWaitForRefill / 1_000_000_000.0).toLong()
        return tooManyRequests(exchange, retryAfterSeconds)
    }

    private fun buildBucket(cfg: RateLimitProperties.EndpointLimit): Bucket =
        Bucket.builder()
            .addLimit { limit ->
                limit.capacity(cfg.capacity)
                    .refillGreedy(cfg.capacity, cfg.refillPeriod)
            }
            .build()

    private fun resolveClientIp(request: ServerHttpRequest): String {
        val xff = request.headers.getFirst("X-Forwarded-For")
        if (!xff.isNullOrBlank()) {
            val first = xff.split(",").firstOrNull()?.trim()
            if (!first.isNullOrEmpty()) return first
        }
        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }

    private fun tooManyRequests(exchange: ServerWebExchange, retryAfterSeconds: Long): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.add(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
        response.headers.contentType = MediaType.APPLICATION_JSON
        val json = """{"error":"Too Many Requests","retryAfterSeconds":$retryAfterSeconds}"""
        val buffer = response.bufferFactory().wrap(json.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(buffer))
    }
}
```

- [ ] **Step 4: Запустить тесты — убедиться, что все проходят**

Run: `cd backend && mvn -q test -Dtest=AuthRateLimitWebFilterTest`
Expected: PASS (7 тестов).

- [ ] **Step 5: Прогнать весь тестовый набор (регрессия)**

Run: `cd backend && mvn -q test -Dtest=RateLimitPropertiesTest,AuthRateLimitWebFilterTest`
Expected: PASS (9 тестов суммарно).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/kotlin/com/example/webflux/config/AuthRateLimitWebFilter.kt backend/src/test/kotlin/com/example/webflux/config/AuthRateLimitWebFilterTest.kt
git commit -m "feat: add reactive bucket4j rate limit WebFilter for auth endpoints"
```

---

### Task 4: Конфигурация в application.yml + ручная проверка

**Files:**
- Modify: `backend/src/main/resources/application.yml` (добавить секцию `rate-limit` верхнего уровня, после блока `ai-agent`)

**Interfaces:**
- Consumes: `RateLimitProperties` (Task 2), `AuthRateLimitWebFilter` (Task 3).
- Produces: рантайм-значения лимитов для фильтра.

- [ ] **Step 1: Добавить секцию `rate-limit` в `application.yml`**

В конец `backend/src/main/resources/application.yml` (новый top-level блок, не вложенный в `ai-agent`):

```yaml
rate-limit:
  # Глобальный выключатель rate limiting для эндпоинтов аутентификации
  enabled: true
  # Время простоя, после которого бакет вытесняется из Caffeine-кэша.
  # ДОЛЖНО быть >= максимального refill-period ниже (register = 1h).
  idle-timeout: 1h
  endpoints:
    # Вход: 5 попыток в минуту на IP (защита от брутфорса пароля)
    login:
      capacity: 5
      refill-period: 1m
    # Регистрация: 3 в час на IP (защита от массового создания аккаунтов)
    register:
      capacity: 3
      refill-period: 1h
    # Выход: 10 в минуту на IP
    logout:
      capacity: 10
      refill-period: 1m
```

- [ ] **Step 2: Проверить, что проект собирается с новой конфигурацией**

Run: `cd backend && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Ручная smoke-проверка (требует запущенный PostgreSQL и приложение)**

> Это ручной end-to-end шаг. Автотесты фильтра (Task 3) уже покрывают логику; здесь проверяем, что yml подхватывается и фильтр активен в полном приложении.

Запустить приложение: `cd backend && mvn spring-boot:run`

В отдельном окне PowerShell выполнить 6 запросов на login подряд:

```powershell
1..6 | ForEach-Object {
  $r = Invoke-WebRequest -Uri http://localhost:8080/api/auth/login -Method Post `
        -ContentType 'application/json' `
        -Body '{"email":"nobody@example.com","password":"WrongPass123!"}' `
        -SkipHttpErrorCheck
  "Attempt $_`: HTTP $($r.StatusCode)  Retry-After=$($r.Headers['Retry-After'])"
}
```

Expected: попытки 1–5 → `HTTP 401` (неверные креды, но запрос пропущен фильтром);
попытка 6 → `HTTP 429` с непустым заголовком `Retry-After`.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "feat: configure strict auth rate limits in application.yml"
```

---

## Self-Review

**1. Spec coverage:**
- Зависимости bucket4j + Caffeine → Task 1. ✔
- `RateLimitProperties` (enabled, idleTimeout, endpoints) + binding → Task 2. ✔
- `application.yml` секция `rate-limit` со строгими лимитами → Task 4. ✔
- `AuthRateLimitWebFilter`: `@Order(HIGHEST_PRECEDENCE)`, матчинг 3 POST-путей, `enabled=false` bypass, IP из XFF→remoteAddress, Caffeine `expireAfterAccess`, greedy refill, `tryConsumeAndReturnRemaining`, `X-Rate-Limit-Remaining`, 429 + `Retry-After` + JSON-тело → Task 3. ✔
- Очистка памяти через Caffeine (вместо @Scheduled) → Task 3 (cache builder). ✔
- Тесты (7 сценариев спеки) → Task 3 Step 1 (7 тестов: capacity→429, заголовки/тело, изоляция IP, независимость эндпоинтов, disabled, нецелевые запросы, приоритет XFF). ✔

**2. Placeholder scan:** плейсхолдеров нет — весь код приведён полностью, команды и ожидаемый вывод указаны.

**3. Type consistency:** `RateLimitProperties` / `RateLimitProperties.EndpointLimit(capacity: Long, refillPeriod: Duration)` используются одинаково в Task 2 и Task 3. Конструктор фильтра `AuthRateLimitWebFilter(properties: RateLimitProperties)` совпадает в тестах и реализации. Имена YAML-ключей (`idle-timeout`, `refill-period`, `endpoints.login/register/logout`) согласованы между Task 2 (тест), Task 4 (yml) и Task 3 (чтение `properties.endpoints[...]`).
