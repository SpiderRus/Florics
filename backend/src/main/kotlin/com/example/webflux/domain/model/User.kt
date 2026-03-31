package com.example.webflux.domain.model

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val password: String,
    val roles: Set<String> = setOf("USER")
)
