package com.example.huellitasya.core.domain.validation

import com.example.huellitasya.core.domain.error.ValidationError
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validadores de formularios (specs 002 y 004). Devuelven `null` si el valor es válido.
 * Son lógica pura: no dependen de Android para poder probarse en la JVM.
 */
class ValidateNameUseCase @Inject constructor() {
    operator fun invoke(name: String): ValidationError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.EmptyName
            trimmed.length < MIN_NAME_LENGTH -> ValidationError.NameTooShort
            trimmed.length > MAX_NAME_LENGTH -> ValidationError.NameTooLong
            else -> null
        }
    }

    private companion object {
        const val MIN_NAME_LENGTH = 2

        /** Mismo límite que `firebase/firestore.rules` (spec 004). */
        const val MAX_NAME_LENGTH = 50
    }
}

/** Teléfono opcional (spec 004): `+` opcional y de 7 a 15 dígitos; se aceptan espacios y guiones. */
class ValidatePhoneUseCase @Inject constructor() {
    operator fun invoke(phone: String): ValidationError? {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return null
        val digits = trimmed.count { it.isDigit() }
        return if (PHONE_REGEX.matches(trimmed) && digits in MIN_DIGITS..MAX_DIGITS) null else ValidationError.InvalidPhone
    }

    /** Forma en que se guarda: sin espacios ni guiones (`+57 312-445` → `+57312445`). */
    fun normalize(phone: String): String = phone.filter { it.isDigit() || it == '+' }

    private companion object {
        val PHONE_REGEX = Regex("^\\+?[0-9][0-9 -]*$")
        const val MIN_DIGITS = 7
        const val MAX_DIGITS = 15
    }
}

/** Dirección opcional (spec 004): máximo 100 caracteres. */
class ValidateAddressUseCase @Inject constructor() {
    operator fun invoke(address: String): ValidationError? =
        if (address.trim().length > MAX_ADDRESS_LENGTH) ValidationError.AddressTooLong else null

    private companion object {
        const val MAX_ADDRESS_LENGTH = 100
    }
}

/** Fecha de nacimiento opcional (spec 004): ni futura ni anterior a 1900. */
class ValidateBirthDateUseCase @Inject constructor() {
    operator fun invoke(date: LocalDate?, today: LocalDate = LocalDate.now()): ValidationError? = when {
        date == null -> null
        date.isAfter(today) || date.isBefore(MIN_DATE) -> ValidationError.InvalidBirthDate
        else -> null
    }

    private companion object {
        val MIN_DATE: LocalDate = LocalDate.of(1900, 1, 1)
    }
}

class ValidateEmailUseCase @Inject constructor() {
    operator fun invoke(email: String): ValidationError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.EmptyEmail
            !EMAIL_REGEX.matches(trimmed) -> ValidationError.InvalidEmail
            else -> null
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

/** Estado de cada requisito de la contraseña (Figma "Crear nueva contraseña"). */
data class PasswordRequirements(
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasDigitOrSymbol: Boolean,
    val matchesConfirmation: Boolean,
) {
    val allMet: Boolean get() = hasMinLength && hasUppercase && hasDigitOrSymbol && matchesConfirmation
}

class ValidatePasswordUseCase @Inject constructor() {
    /**
     * Reglas de registro (Figma "CREAR CONTRASEÑA"): mínimo 8 caracteres, una letra
     * mayúscula y un número o carácter especial.
     */
    fun forRegistration(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.EmptyPassword
        password.length < MIN_PASSWORD_LENGTH -> ValidationError.PasswordTooShort
        password.none { it.isUpperCase() } -> ValidationError.PasswordNeedsUppercase
        password.none { it.isDigit() || !it.isLetterOrDigit() } -> ValidationError.PasswordNeedsDigitOrSymbol
        else -> null
    }

    /** Mismas reglas que [forRegistration], una por una, para mostrarlas en vivo. */
    fun requirements(password: String, confirmation: String): PasswordRequirements = PasswordRequirements(
        hasMinLength = password.length >= MIN_PASSWORD_LENGTH,
        hasUppercase = password.any { it.isUpperCase() },
        hasDigitOrSymbol = password.any { it.isDigit() || !it.isLetterOrDigit() },
        matchesConfirmation = password.isNotEmpty() && password == confirmation,
    )

    /** En login solo se exige que no esté vacía: la verificación real la hace Firebase. */
    fun forLogin(password: String): ValidationError? =
        if (password.isEmpty()) ValidationError.EmptyPassword else null

    fun confirmation(password: String, confirmation: String): ValidationError? = when {
        confirmation.isEmpty() -> ValidationError.EmptyPassword
        password != confirmation -> ValidationError.PasswordsDoNotMatch
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
