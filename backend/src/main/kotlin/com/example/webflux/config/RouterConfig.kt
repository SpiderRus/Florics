package com.example.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class RouterConfig {

    @Bean
    fun staticRouter(): RouterFunction<ServerResponse> = router {
        // Главная страница
        GET("/") {
            ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(ClassPathResource("static/index.html"))
        }

        // Fallback для SPA роутинга - все не-API маршруты возвращают index.html
        GET("/{path:^(?!api|swagger|actuator).*$}") {
            ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(ClassPathResource("static/index.html"))
        }
    }
}
