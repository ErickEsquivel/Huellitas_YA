package com.example.huellitasya.core.domain.error

import com.example.huellitasya.core.common.AppError

/** Errores de validación en cliente (tablas "Reglas de validación" de las specs 002, 004 y 005). */
enum class ValidationError : AppError {
    EmptyName,
    NameTooShort,
    NameTooLong,
    EmptyEmail,
    InvalidEmail,
    EmptyPassword,
    PasswordTooShort,
    PasswordNeedsUppercase,
    PasswordNeedsDigitOrSymbol,
    PasswordsDoNotMatch,
    TermsNotAccepted,
    InvalidPhone,
    AddressTooLong,
    InvalidBirthDate,
    EmptySpecies,
    EmptyBreed,
    BreedTooShort,
    BreedTooLong,
    EmptyBirthDate,
    EmptySex,
    EmptyWeight,
    InvalidWeight,
}

/** `true` si el error se debe a un campo vacío (la UI lo agrupa en "Rellene todos los datos"). */
val ValidationError.isMissingValue: Boolean
    get() = this == ValidationError.EmptyName ||
        this == ValidationError.EmptyEmail ||
        this == ValidationError.EmptyPassword ||
        this == ValidationError.EmptySpecies ||
        this == ValidationError.EmptyBreed ||
        this == ValidationError.EmptyBirthDate ||
        this == ValidationError.EmptySex ||
        this == ValidationError.EmptyWeight
