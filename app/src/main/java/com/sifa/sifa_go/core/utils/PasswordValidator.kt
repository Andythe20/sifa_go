package com.sifa.sifa_go.core.utils

object PasswordValidator {

    data class Requirement(
        val label: String,
        val isValid: Boolean
    )

    fun validate(password: String): List<Requirement> = listOf(
        Requirement(
            label = "Al menos 8 caracteres",
            isValid = password.length >= 8
        ),
        Requirement(
            label = "Al menos una mayúscula",
            isValid = password.any { it.isUpperCase() }
        ),
        Requirement(
            label = "Al menos una minúscula",
            isValid = password.any { it.isLowerCase() }
        ),
        Requirement(
            label = "Al menos un número",
            isValid = password.any { it.isDigit() }
        )
    )

    fun isFullyValid(password: String): Boolean =
        validate(password).all { it.isValid }
}
