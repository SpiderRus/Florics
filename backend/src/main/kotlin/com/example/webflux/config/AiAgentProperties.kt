package com.example.webflux.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties для AI Agent сервиса
 *
 * Биндится из application.yml секции ai-agent.
 * Позволяет настраивать base URL, таймауты и параметры connection pool.
 */
@ConfigurationProperties(prefix = "ai-agent")
data class AiAgentProperties(
    /**
     * Base URL AI Agent сервиса (например http://localhost:8081)
     */
    val baseUrl: String = "http://localhost:8081",

    /**
     * Base path API (например /api/v1)
     */
    val basePath: String = "/api/v1",

    /**
     * Таймаут установки соединения в миллисекундах
     */
    val connectTimeout: Long = 5000,

    /**
     * Таймаут чтения ответа в миллисекундах
     * Увеличен до 30 секунд для обработки длительных AI генераций
     */
    val readTimeout: Long = 30000,

    /**
     * Настройки connection pool
     */
    val pool: PoolProperties = PoolProperties()
) {
    data class PoolProperties(
        /**
         * Максимальное количество соединений в pool
         */
        val maxConnections: Int = 10,

        /**
         * Таймаут ожидания соединения из pool в миллисекундах
         */
        val pendingAcquireTimeout: Long = 45000
    )
}
