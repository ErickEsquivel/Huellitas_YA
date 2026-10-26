package com.example.huellitasya.core.domain.validation

import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.MAX_RECORD_BYTES
import com.example.huellitasya.core.model.PDF_CONTENT_TYPE
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validadores de formularios (specs 002, 004, 005 y 006). Devuelven `null` si el valor es válido.
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

/** Raza de la mascota (spec 005): obligatoria, de 2 a 50 caracteres. */
class ValidatePetBreedUseCase @Inject constructor() {
    operator fun invoke(breed: String): ValidationError? {
        val trimmed = breed.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.EmptyBreed
            trimmed.length < MIN_LENGTH -> ValidationError.BreedTooShort
            trimmed.length > MAX_LENGTH -> ValidationError.BreedTooLong
            else -> null
        }
    }

    private companion object {
        const val MIN_LENGTH = 2

        /** Mismo límite que `firebase/firestore.rules`. */
        const val MAX_LENGTH = 50
    }
}

/** Fecha de nacimiento de la mascota (spec 005): obligatoria, ni futura ni anterior a 1990. */
class ValidatePetBirthDateUseCase @Inject constructor() {
    operator fun invoke(date: LocalDate?, today: LocalDate = LocalDate.now()): ValidationError? = when {
        date == null -> ValidationError.EmptyBirthDate
        date.isAfter(today) || date.isBefore(MIN_DATE) -> ValidationError.InvalidBirthDate
        else -> null
    }

    companion object {
        val MIN_DATE: LocalDate = LocalDate.of(1990, 1, 1)
    }
}

/** Peso de la mascota en kg (spec 005): obligatorio, mayor que 0 y hasta 150. Acepta punto o coma. */
class ValidatePetWeightUseCase @Inject constructor() {
    operator fun invoke(weight: String): ValidationError? {
        if (weight.isBlank()) return ValidationError.EmptyWeight
        val value = parse(weight) ?: return ValidationError.InvalidWeight
        return if (value > 0.0 && value <= MAX_WEIGHT_KG) null else ValidationError.InvalidWeight
    }

    /** `"5,5"` → `5.5`; `null` si no es un número. */
    fun parse(weight: String): Double? = weight.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    private companion object {
        /** Mismo límite que `firebase/firestore.rules`. */
        const val MAX_WEIGHT_KG = 150.0
    }
}

/** Archivo de un registro médico (spec 006): PDF o imagen, de menos de 10 MB. */
class ValidateRecordFileUseCase @Inject constructor() {
    operator fun invoke(file: LocalFileInfo): ValidationError? = when {
        file.contentType != PDF_CONTENT_TYPE && !file.contentType.startsWith("image/") -> ValidationError.UnsupportedFileType
        file.sizeBytes <= 0 || file.sizeBytes >= MAX_RECORD_BYTES -> ValidationError.FileTooBig
        else -> null
    }
}

/** Datos del formulario "Subir Archivo" (spec 006): fecha no futura ni anterior a 1990; veterinario de hasta 50. */
class ValidateRecordFormUseCase @Inject constructor() {
    fun date(date: LocalDate, today: LocalDate = LocalDate.now()): ValidationError? =
        if (date.isAfter(today) || date.isBefore(ValidatePetBirthDateUseCase.MIN_DATE)) ValidationError.InvalidBirthDate else null

    fun vet(vet: String): ValidationError? = if (vet.trim().length > MAX_VET_LENGTH) ValidationError.VetTooLong else null

    private companion object {
        /** Mismo límite que `firebase/firestore.rules`. */
        const val MAX_VET_LENGTH = 50
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
