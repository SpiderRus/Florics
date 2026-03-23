package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект информации о webhook Telegram
 * https://core.telegram.org/bots/api#webhookinfo
 */
@Schema(description = "Информация о webhook Telegram")
data class WebhookInfoDto(
    @Schema(description = "URL webhook, может быть пустым, если webhook не настроен")
    @JsonProperty("url")
    val url: String,

    @Schema(description = "True, если для проверки сертификата webhook был предоставлен пользовательский сертификат")
    @JsonProperty("has_custom_certificate")
    val hasCustomCertificate: Boolean,

    @Schema(description = "Количество обновлений, ожидающих доставки")
    @JsonProperty("pending_update_count")
    val pendingUpdateCount: Int,

    @Schema(description = "Используемый в данный момент IP-адрес webhook")
    @JsonProperty("ip_address")
    val ipAddress: String? = null,

    @Schema(description = "Unix time последней ошибки, произошедшей при попытке доставить обновление")
    @JsonProperty("last_error_date")
    val lastErrorDate: Long? = null,

    @Schema(description = "Сообщение об ошибке в читаемом формате")
    @JsonProperty("last_error_message")
    val lastErrorMessage: String? = null,

    @Schema(description = "Unix time последней ошибки, произошедшей при попытке синхронизации")
    @JsonProperty("last_synchronization_error_date")
    val lastSynchronizationErrorDate: Long? = null,

    @Schema(description = "Максимально допустимое количество одновременных HTTPS-соединений с webhook")
    @JsonProperty("max_connections")
    val maxConnections: Int? = null,

    @Schema(description = "Список типов обновлений, на которые подписан бот")
    @JsonProperty("allowed_updates")
    val allowedUpdates: List<String>? = null
)
