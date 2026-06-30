package com.example.webflux.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration

class RateLimitPropertiesTest {

    @EnableConfigurationProperties(RateLimitProperties::class)
    class TestConfig

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)

    @Test
    fun `binds enabled, idleTimeout and per-endpoint limits`() {
        runner.withPropertyValues(
            "rate-limit.enabled=true",
            "rate-limit.idle-timeout=1h",
            "rate-limit.endpoints.login.capacity=5",
            "rate-limit.endpoints.login.refill-period=1m",
            "rate-limit.endpoints.register.capacity=3",
            "rate-limit.endpoints.register.refill-period=1h",
            "rate-limit.endpoints.logout.capacity=10",
            "rate-limit.endpoints.logout.refill-period=1m",
        ).run { ctx ->
            val props = ctx.getBean(RateLimitProperties::class.java)
            assertThat(props.enabled).isTrue()
            assertThat(props.idleTimeout).isEqualTo(Duration.ofHours(1))
            assertThat(props.endpoints["login"]!!.capacity).isEqualTo(5L)
            assertThat(props.endpoints["login"]!!.refillPeriod).isEqualTo(Duration.ofMinutes(1))
            assertThat(props.endpoints["register"]!!.refillPeriod).isEqualTo(Duration.ofHours(1))
            assertThat(props.endpoints["logout"]!!.capacity).isEqualTo(10L)
        }
    }

    @Test
    fun `applies defaults when not specified`() {
        runner.run { ctx ->
            val props = ctx.getBean(RateLimitProperties::class.java)
            assertThat(props.enabled).isTrue()
            assertThat(props.idleTimeout).isEqualTo(Duration.ofHours(1))
            assertThat(props.endpoints).isEmpty()
        }
    }
}
