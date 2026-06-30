package com.example.webflux.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpResponse
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.time.Duration

class AuthRateLimitWebFilterTest {

    private val strictProps = RateLimitProperties(
        enabled = true,
        idleTimeout = Duration.ofHours(1),
        endpoints = mapOf(
            "login" to RateLimitProperties.EndpointLimit(5, Duration.ofMinutes(1)),
            "register" to RateLimitProperties.EndpointLimit(3, Duration.ofHours(1)),
            "logout" to RateLimitProperties.EndpointLimit(10, Duration.ofMinutes(1)),
        )
    )

    private class CountingChain : WebFilterChain {
        var count = 0
        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            count++
            return Mono.empty()
        }
    }

    private fun callPost(
        filter: AuthRateLimitWebFilter,
        path: String,
        xff: String? = "1.1.1.1",
        remote: String = "9.9.9.9",
    ): ServerWebExchange {
        var builder = MockServerHttpRequest.post(path)
            .remoteAddress(InetSocketAddress(remote, 40000))
        if (xff != null) builder = builder.header("X-Forwarded-For", xff)
        val exchange = MockServerWebExchange.from(builder.build())
        filter.filter(exchange, CountingChain()).block()
        return exchange
    }

    private fun callGet(filter: AuthRateLimitWebFilter, path: String): ServerWebExchange {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build())
        filter.filter(exchange, CountingChain()).block()
        return exchange
    }

    @Test
    fun `allows up to capacity then returns 429 on login`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) {
            val ex = callPost(filter, "/api/auth/login")
            assertThat(ex.response.statusCode).isNull() // прошло дальше по цепочке
        }
        val blocked = callPost(filter, "/api/auth/login")
        assertThat(blocked.response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun `429 response has Retry-After header and json body`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) { callPost(filter, "/api/auth/login") }
        val blocked = callPost(filter, "/api/auth/login")

        assertThat(blocked.response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(blocked.response.headers.getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank()
        val body = (blocked.response as MockServerHttpResponse).bodyAsString.block()
        assertThat(body).contains("Too Many Requests").contains("retryAfterSeconds")
    }

    @Test
    fun `different IPs do not share a bucket`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) { callPost(filter, "/api/auth/login", xff = "1.1.1.1") }
        // другой IP — отдельный бакет, проходит
        val other = callPost(filter, "/api/auth/login", xff = "2.2.2.2")
        assertThat(other.response.statusCode).isNull()
    }

    @Test
    fun `endpoints have independent counters`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        repeat(5) { callPost(filter, "/api/auth/login") }       // login исчерпан
        // register всё ещё разрешён (3 токена), logout тоже
        assertThat(callPost(filter, "/api/auth/register").response.statusCode).isNull()
        assertThat(callPost(filter, "/api/auth/logout").response.statusCode).isNull()
        // а login заблокирован
        assertThat(callPost(filter, "/api/auth/login").response.statusCode)
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun `disabled filter never limits`() {
        val filter = AuthRateLimitWebFilter(strictProps.copy(enabled = false))
        repeat(50) {
            assertThat(callPost(filter, "/api/auth/login").response.statusCode).isNull()
        }
    }

    @Test
    fun `non-target requests are untouched`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        // GET на тот же путь — не цель
        repeat(20) {
            assertThat(callGet(filter, "/api/auth/login").response.statusCode).isNull()
        }
        // POST на нецелевой путь — не цель
        repeat(20) {
            assertThat(callPost(filter, "/api/goods").response.statusCode).isNull()
        }
    }

    @Test
    fun `X-Forwarded-For takes priority over remoteAddress`() {
        val filter = AuthRateLimitWebFilter(strictProps)
        // одинаковый remoteAddress, разные XFF — должны быть разные бакеты
        repeat(5) { callPost(filter, "/api/auth/login", xff = "1.1.1.1", remote = "9.9.9.9") }
        val blockedSameXff = callPost(filter, "/api/auth/login", xff = "1.1.1.1", remote = "9.9.9.9")
        assertThat(blockedSameXff.response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        val otherXff = callPost(filter, "/api/auth/login", xff = "2.2.2.2", remote = "9.9.9.9")
        assertThat(otherXff.response.statusCode).isNull()
    }
}
