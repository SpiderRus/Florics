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
