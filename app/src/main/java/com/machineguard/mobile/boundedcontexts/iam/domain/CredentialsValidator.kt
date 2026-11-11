package com.machineguard.mobile.boundedcontexts.iam.domain

enum class EmailError { Empty, Invalid }

enum class PasswordError { Empty }

/** Validación sintáctica del formulario. Las reglas de negocio las decide el Core API. */
object CredentialsValidator {
    private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateEmail(email: String): EmailError? {
        val value = email.trim()
        return when {
            value.isEmpty() -> EmailError.Empty
            !EMAIL_PATTERN.matches(value) -> EmailError.Invalid
            else -> null
        }
    }

    fun validatePassword(password: String): PasswordError? =
        if (password.isBlank()) PasswordError.Empty else null
}
