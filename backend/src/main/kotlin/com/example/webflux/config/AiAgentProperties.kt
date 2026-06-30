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
     * Base path API нового бота OllamaTestController (например /api/v1/ollama)
     */
    val basePath: String = "/api/v1/ollama",

    /**
     * Тип агента (agents.prompts.<agentType> в AIAgentNew).
     * Для магазина растений — "plants".
     */
    val agentType: String = "plants",

    /**
     * Тип агента для дизайнера флорариумов (генерация картинок).
     * Используется чатом на странице «Создание своего флорариума».
     */
    val florariumAgentType: String = "florarium",

    /**
     * Тип агента-эксперта (ассистент мастера по сборке флорариума).
     * Используется в админке (вкладка «Заказы») для консультаций по заказу.
     */
    val florariumExpertAgentType: String = "florarium-expert",

    /**
     * Тип агента-анализатора фото растений (заполняет карточку товара по изображениям).
     */
    val photoAnalyzerAgentType: String = "photo-analyzer",

    /**
     * Таймаут установки соединения в миллисекундах
     */
    val connectTimeout: Long = 5000,

    /**
     * Таймаут чтения ответа в миллисекундах.
     * Увеличен до 120 секунд: новый бот дольше отдаёт первый токен (RAG + tool-calls).
     */
    val readTimeout: Long = 120000,

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
