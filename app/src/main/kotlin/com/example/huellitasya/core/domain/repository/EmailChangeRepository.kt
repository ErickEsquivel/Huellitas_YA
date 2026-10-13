package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.EmailChangeError

/**
 * Cambio de correo con un código de 4 dígitos (spec 004). Lo implementa el backend (Cloud
 * Functions): envía el código desde el Gmail "noreply" y cambia el correo con el Admin SDK.
 */
interface EmailChangeRepository {
    /** Envía el código a [newEmail]. Exige un inicio de sesión reciente. */
    suspend fun requestCode(newEmail: String): AppResult<Unit, EmailChangeError>

    /** Valida el código y cambia el correo en Auth y en `users/{uid}`. Devuelve el correo nuevo. */
    suspend fun confirm(code: String): AppResult<String, EmailChangeError>
}
