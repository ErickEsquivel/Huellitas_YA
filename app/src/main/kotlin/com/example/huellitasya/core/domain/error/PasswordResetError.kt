package com.example.huellitasya.core.domain.error

import com.example.huellitasya.core.common.AppError

/** Errores del restablecimiento de contraseña con código (spec 002, "Recuperar contraseña"). */
sealed interface PasswordResetError : AppError {
    /** El código no coincide. */
    data object InvalidCode : PasswordResetError

    /** El código (o la verificación) venció: hay que pedir uno nuevo. */
    data object ExpiredCode : PasswordResetError

    /** Se agotaron los intentos para este código. */
    data object TooManyAttempts : PasswordResetError

    /** Se pidió otro código demasiado pronto o demasiadas veces. */
    data class TooManyRequests(val retryAfterSeconds: Int) : PasswordResetError

    /** El servidor rechazó la contraseña por no cumplir la política. */
    data object WeakPassword : PasswordResetError

    data object Network : PasswordResetError

    data object Unknown : PasswordResetError
}
