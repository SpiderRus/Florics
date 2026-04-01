package com.example.webflux.config

import com.example.webflux.security.AuthenticationService
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun opaqueTokenIntrospector(authenticationService: AuthenticationService): ReactiveOpaqueTokenIntrospector {
        return ReactiveOpaqueTokenIntrospector { token: String ->
            mono { authenticationService.validateToken(token) }
                .flatMap { tokenInfo ->
                    val authorities = tokenInfo.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                    val attributes = mapOf(
                            "sub" to tokenInfo.userId.toString(),
                            "email" to tokenInfo.email,
                            "token_info" to tokenInfo
                        )

                    Mono.just<OAuth2AuthenticatedPrincipal>(
                        DefaultOAuth2AuthenticatedPrincipal(tokenInfo.email, attributes, authorities)
                    )
                }
                .switchIfEmpty(Mono.error(
                    OAuth2AuthenticationException(OAuth2Error("invalid_token", "Invalid or expired token", null))
                ))
        }
    }

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        tokenIntrospector: ReactiveOpaqueTokenIntrospector
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .exceptionHandling { spec ->
                spec.authenticationEntryPoint { exchange, _ ->
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    exchange.response.headers.add("WWW-Authenticate", "Bearer")
                    exchange.response.setComplete()
                }
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.opaqueToken { opaque ->
                    opaque.introspector(tokenIntrospector)
                }
            }
            .authorizeExchange { exchanges ->
                exchanges
                    // Публичные эндпоинты (login и register)
                    .pathMatchers("/api/auth/login", "/api/auth/register").permitAll()
                    .pathMatchers("/api/goods/**").permitAll()
                    .pathMatchers("/api/categories/**").permitAll()
                    .pathMatchers("/api/reviews/{goodsId}").permitAll()
                    .pathMatchers("/api/reviews/rating/{goodsId}").permitAll()
                    .pathMatchers("/api/hello/**").permitAll()
                    .pathMatchers("/api/stream").permitAll()
                    .pathMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                    .pathMatchers("/api-docs/**", "/v3/api-docs/**").permitAll()
                    .pathMatchers("/webjars/**").permitAll()
                    // Статические ресурсы фронтенда
                    .pathMatchers("/", "/index.html", "/assets/**", "/*.js", "/*.css", "/*.ico").permitAll()
                    .pathMatchers("/catalog", "/catalog/*", "/login", "/register", "/cart", "/profile", "/terrariums",
                        "/custom-terrarium", "/masterclasses", "/masterclass/*").permitAll()
                    // Остальное требует авторизацию (включая /api/auth/me и /api/auth/logout)
                    .anyExchange().authenticated()
            }
            .build()
    }
}
