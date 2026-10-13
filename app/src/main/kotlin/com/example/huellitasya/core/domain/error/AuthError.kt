package com.example.huellitasya.core.domain.error

import com.example.huellitasya.core.common.AppError

/** Errores remotos de autenticación (spec 002, tabla "Errores remotos"). */
enum class AuthError : AppError {
    InvalidCredentials,
    EmailAlreadyInUse,
    WeakPassword,
    Network,
    TooManyRequests,
    /** El usuario cerró el selector de cuentas de Google: no se muestra mensaje. */
    Cancelled,
    GoogleSignInFailed,
    Unknown,
}

/** Errores de lectura/escritura de datos (Firestore). */
enum class DataError : AppError {
    Network,
    PermissionDenied,
    /** El documento `users/{uid}` no existe (spec 004). */
    NotFound,
    /** La foto pesa más de 5 MB aun comprimida (límite de `storage.rules`). */
    FileTooLarge,
    Unknown,
}
