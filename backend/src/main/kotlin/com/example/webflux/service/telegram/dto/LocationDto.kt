package com.example.webflux.service.telegram.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Объект местоположения Telegram
 * https://core.telegram.org/bots/api#location
 */
@Schema(description = "Местоположение Telegram")
data class LocationDto(
    @Schema(description = "Долгота, определенная отправителем")
    @JsonProperty("longitude")
    val longitude: Double,

    @Schema(description = "Широта, определенная отправителем")
    @JsonProperty("latitude")
    val latitude: Double,

    @Schema(description = "Радиус неопределенности местоположения, измеряемый в метрах")
    @JsonProperty("horizontal_accuracy")
    val horizontalAccuracy: Double? = null,

    @Schema(description = "Время относительно даты отправки сообщения, в течение которого местоположение может обновляться")
    @JsonProperty("live_period")
    val livePeriod: Int? = null,

    @Schema(description = "Направление, в котором движется пользователь, в градусах")
    @JsonProperty("heading")
    val heading: Int? = null,

    @Schema(description = "Максимальное расстояние для оповещений о приближении к другому участнику чата")
    @JsonProperty("proximity_alert_radius")
    val proximityAlertRadius: Int? = null
)
