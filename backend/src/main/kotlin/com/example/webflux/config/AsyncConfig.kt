package com.example.webflux.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AsyncConfig {
    @Bean
    fun asyncJobScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())
}