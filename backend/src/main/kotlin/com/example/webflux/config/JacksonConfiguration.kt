package com.example.webflux.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer

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
