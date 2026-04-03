package com.example.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import reactor.core.publisher.Hooks

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()

    runApplication<Application>(*args)
}
