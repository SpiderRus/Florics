package com.example.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.path
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class RouterConfig {

    @Bean
    fun staticRouter(): RouterFunction<ServerResponse> {
        val spaRouter = router {
            GET("/") {
                ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(ClassPathResource("static/index.html"))
            }
            
            GET("/{*path}") {
                ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(ClassPathResource("static/index.html"))
            }
        }
        
        // Исключаем из роутера все пути API и статики
        return RouterFunctions.nest(
            path("/api/**").or(path("/swagger**")).or(path("/actuator/**")).or(path("/assets/**")).negate(),
            spaRouter
        )
    }
}
