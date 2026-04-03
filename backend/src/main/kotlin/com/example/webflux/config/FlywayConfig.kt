package com.example.webflux.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация Flyway: JDBC-источник данных строится из настроек R2DBC (URL преобразуется в jdbc:).
 */
@Configuration
class FlywayConfig {
    @Bean(initMethod = "migrate")
    fun flyway(
        @Value("\${spring.r2dbc.url}") r2dbcUrl: String,
        @Value("\${spring.r2dbc.username:}") username: String?,
        @Value("\${spring.r2dbc.password:}") password: String?,
        @Value("\${spring.flyway.locations:classpath:db/migration}") locations: String
    ): Flyway {
        return Flyway.configure()
            .dataSource(toFlywayUrl(r2dbcUrl), username, password)
            .locations(*locations.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            .baselineOnMigrate(true)
            .load()
    }

    private companion object {
        /** Преобразует R2DBC URL в JDBC URL для Flyway.  */
        fun toFlywayUrl(r2dbcUrl: String): String {
            return r2dbcUrl.replaceFirst("^r2dbc:(pool:)?".toRegex(), "jdbc:")
        }
    }
}
