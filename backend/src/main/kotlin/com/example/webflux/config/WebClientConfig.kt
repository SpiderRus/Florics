package com.example.webflux.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Конфигурация WebClient для взаимодействия с AI Agent сервисом
 *
 * Создает настроенный WebClient bean с:
 * - Connection pool и таймаутами
 * - Базовым URL и default headers
 * - Логированием запросов/ответов
 */
@Configuration
@EnableConfigurationProperties(AiAgentProperties::class)
class WebClientConfig(
    private val properties: AiAgentProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WebClientConfig::class.java)
    }

    /**
     * WebClient bean для AI Agent сервиса
     *
     * Настроен с:
     * - Base URL: http://localhost:8081/api/v1
     * - Connect timeout: 5 секунд
     * - Read timeout: 30 секунд (для длительных AI генераций)
     * - Connection pool: max 10 connections, max 50 pending requests
     * - DEBUG логирование всех запросов
     */
    @Bean
    @Qualifier("aiAgentWebClient")
    fun aiAgentWebClient(): WebClient {
        // Настройка connection pool
        val connectionProvider = ConnectionProvider.builder("ai-agent-pool")
            .maxConnections(properties.pool.maxConnections)
            .pendingAcquireTimeout(Duration.ofMillis(properties.pool.pendingAcquireTimeout))
            .maxIdleTime(Duration.ofSeconds(20))  // Закрывать idle соединения через 20 сек
            .maxLifeTime(Duration.ofMinutes(5))   // Максимальное время жизни соединения
            .evictInBackground(Duration.ofSeconds(30))  // Фоновая очистка каждые 30 сек
            .build()

        val httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeout.toInt())
            .responseTimeout(Duration.ofMillis(properties.readTimeout))
            .doOnConnected { connection ->
                connection
                    .addHandlerLast(ReadTimeoutHandler(properties.readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(properties.readTimeout, TimeUnit.MILLISECONDS))
            }

        logger.info("AI Agent connection pool configured: maxConnections={}, pendingAcquireTimeout={}ms",
            properties.pool.maxConnections, properties.pool.pendingAcquireTimeout)

        val loggingFilter = ExchangeFilterFunction.ofRequestProcessor { request ->
            logger.debug("AI Agent Request: {} {}", request.method(), request.url())
            Mono.just(request)
        }

        val baseUrl = "${properties.baseUrl}${properties.basePath}"
        logger.info("Configuring AI Agent WebClient with base URL: {}", baseUrl)

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .filter(loggingFilter)
            .build()
    }
}
