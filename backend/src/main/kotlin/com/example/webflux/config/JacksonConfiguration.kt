package com.example.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.config.WebFluxConfigurer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule


@Configuration
class JacksonConfiguration : WebFluxConfigurer {

//    @Bean
//    @Primary
//    fun objectMapper(): ObjectMapper {
//        return JsonMapper.builder()
//            .addModule(KotlinModule.Builder().build())
//            .build()
//    }
//
//    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
//        val mapper = JsonMapper.builder().build() // Рекомендуемый способ создания
//        configurer.defaultCodecs().jackson2JsonEncoder(JacksonJsonEncoder(mapper))
//        configurer.defaultCodecs().jackson2JsonDecoder(JacksonJsonDecoder(mapper))
//    }
}
