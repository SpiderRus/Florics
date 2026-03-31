package com.example.webflux.security

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Валидатор для проверки сложности пароля.
 *
 * Проверяет, что пароль соответствует требованиям безопасности:
 * - Минимум 8 символов
 * - Содержит хотя бы одну заглавную букву
 * - Содержит хотя бы одну строчную букву
 * - Содержит хотя бы одну цифру
 * - Содержит хотя бы один специальный символ
 */
class PasswordConstraintValidator : ConstraintValidator<ValidPassword, String> {

    companion object {
        // Regex для проверки сложности пароля:
        // (?=.*[a-z]) - хотя бы одна строчная буква
        // (?=.*[A-Z]) - хотя бы одна заглавная буква
        // (?=.*\d) - хотя бы одна цифра
        // (?=.*[@$!%*?&]) - хотя бы один спецсимвол
        // [A-Za-z\d@$!%*?&]{8,} - минимум 8 символов из разрешённых
        private val PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$".toRegex()
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        // Null проверяется отдельной аннотацией @NotBlank
        if (value == null) return false

        return PASSWORD_PATTERN.matches(value)
    }
}
