package com.example.huellitasya.core.domain.error

import com.example.huellitasya.core.common.AppError

/** Errores de validación en cliente (spec 002, tabla "Reglas de validación"). */
enum class ValidationError : AppError {
    EmptyName,
    NameTooShort,
    EmptyEmail,
    InvalidEmail,
    EmptyPassword,
    PasswordTooShort,
    PasswordNeedsUppercase,
    PasswordNeedsDigitOrSymbol,
    PasswordsDoNotMatch,
    TermsNotAccepted,
}

/** `true` si el error se debe a un campo vacío (la UI lo agrupa en "Rellene todos los datos"). */
val ValidationError.isMissingValue: Boolean
    get() = this == ValidationError.EmptyName ||
        this == ValidationError.EmptyEmail ||
        this == ValidationError.EmptyPassword
