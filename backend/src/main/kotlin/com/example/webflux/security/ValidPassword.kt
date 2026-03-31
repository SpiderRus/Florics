package com.example.webflux.security

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Аннотация для валидации сложности пароля.
 *
 * Требования к паролю:
 * - Минимум 8 символов
 * - Хотя бы одна заглавная буква (A-Z)
 * - Хотя бы одна строчная буква (a-z)
 * - Хотя бы одна цифра (0-9)
 * - Хотя бы один специальный символ (@$!%*?&)
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordConstraintValidator::class])
annotation class ValidPassword(
    val message: String = "Пароль должен содержать: минимум 8 символов, заглавную букву, строчную букву, цифру и спецсимвол (@\$!%*?&)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
