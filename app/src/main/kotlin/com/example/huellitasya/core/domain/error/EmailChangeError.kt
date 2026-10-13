package com.example.huellitasya.core.domain.error

import com.example.huellitasya.core.common.AppError

/** Errores del cambio de correo con código (spec 004, "Cambio de correo"). */
sealed interface EmailChangeError : AppError {
    /** El backend exige un inicio de sesión reciente: la app pide la contraseña y reintenta. */
    data object RequiresRecentLogin : EmailChangeError

    /** El correo nuevo ya es de otra cuenta. */
    data object EmailAlreadyInUse : EmailChangeError

    /** El correo nuevo no es válido o es igual al actual. */
    data object InvalidEmail : EmailChangeError

    data object InvalidCode : EmailChangeError

    /** El código venció o ya se usó: hay que pedir otro. */
    data object ExpiredCode : EmailChangeError

    data object TooManyAttempts : EmailChangeError

    /** Se pidió otro código demasiado pronto o demasiadas veces. */
    data class TooManyRequests(val retryAfterSeconds: Int) : EmailChangeError

    data object Network : EmailChangeError

    data object Unknown : EmailChangeError
}
