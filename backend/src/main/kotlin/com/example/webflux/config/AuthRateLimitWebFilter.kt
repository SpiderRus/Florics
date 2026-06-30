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
