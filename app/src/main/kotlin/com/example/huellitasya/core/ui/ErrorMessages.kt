package com.example.huellitasya.core.ui

import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.error.isMissingValue

/** Estado + mensaje que un ViewModel asigna a un campo de formulario. */
data class FieldFeedback(
    val status: FieldStatus,
    val message: UiText? = null,
)

/** Aviso de grupo cuando faltan datos (Figma: "Rellene todos los datos para continuar"). */
val MissingFieldsFeedback = FieldFeedback(FieldStatus.Warning, UiText.Resource(R.string.error_missing_fields))

/** Aviso de grupo para un formulario: [MissingFieldsFeedback] si algún campo quedó vacío. */
fun formFeedbackFor(vararg errors: ValidationError?): FieldFeedback? =
    if (errors.any { it?.isMissingValue == true }) MissingFieldsFeedback else null

/** Error remoto mostrado bajo un campo concreto (borde rojo + ícono X). */
fun AuthError.toFieldFeedback(): FieldFeedback = FieldFeedback(FieldStatus.Error, toUiText())

/**
 * Los campos vacíos solo se resaltan (el mensaje va en [MissingFieldsFeedback]); los datos que
 * no coinciden se muestran como error y el resto como advertencia, igual que en Figma.
 */
fun ValidationError.toFieldFeedback(): FieldFeedback = when {
    isMissingValue -> FieldFeedback(FieldStatus.Warning)
    this == ValidationError.PasswordsDoNotMatch -> FieldFeedback(FieldStatus.Error, toUiText())
    else -> FieldFeedback(FieldStatus.Warning, toUiText())
}

/** Mensajes de las specs 002 y 004 para cada error de validación. */
fun ValidationError.toUiText(): UiText = UiText.Resource(
    when (this) {
        ValidationError.EmptyName -> R.string.error_empty_name
        ValidationError.NameTooShort -> R.string.error_name_too_short
        ValidationError.NameTooLong -> R.string.error_name_too_long
        ValidationError.EmptyEmail -> R.string.error_empty_email
        ValidationError.InvalidEmail -> R.string.error_invalid_email
        ValidationError.EmptyPassword -> R.string.error_empty_password
        ValidationError.PasswordTooShort -> R.string.error_password_too_short
        ValidationError.PasswordNeedsUppercase -> R.string.error_password_uppercase
        ValidationError.PasswordNeedsDigitOrSymbol -> R.string.error_password_digit_or_symbol
        ValidationError.PasswordsDoNotMatch -> R.string.error_passwords_do_not_match
        ValidationError.TermsNotAccepted -> R.string.error_terms_not_accepted
        ValidationError.InvalidPhone -> R.string.error_invalid_phone
        ValidationError.AddressTooLong -> R.string.error_address_too_long
        ValidationError.InvalidBirthDate -> R.string.error_invalid_birth_date
    },
)

/**
 * Mensajes de las specs 002 y 004 para errores remotos. `Cancelled` y `RequiresRecentLogin`
 * no tienen mensaje: devuelven `null`.
 */
fun AuthError.toUiText(): UiText? = when (this) {
    AuthError.Cancelled, AuthError.RequiresRecentLogin -> null
    AuthError.InvalidCredentials -> UiText.Resource(R.string.error_invalid_credentials)
    AuthError.EmailAlreadyInUse -> UiText.Resource(R.string.error_email_in_use)
    AuthError.WeakPassword -> UiText.Resource(R.string.error_weak_password)
    AuthError.Network -> UiText.Resource(R.string.error_network)
    AuthError.TooManyRequests -> UiText.Resource(R.string.error_too_many_requests)
    AuthError.GoogleSignInFailed -> UiText.Resource(R.string.error_google_sign_in)
    AuthError.Unknown -> UiText.Resource(R.string.error_unknown)
}

/** Mensajes del restablecimiento con código. `Network` no tiene mensaje: se muestra el overlay "Sin conexión". */
fun PasswordResetError.toUiText(): UiText? = when (this) {
    PasswordResetError.Network -> null
    PasswordResetError.InvalidCode -> UiText.Resource(R.string.error_reset_code_invalid)
    PasswordResetError.ExpiredCode -> UiText.Resource(R.string.error_reset_code_expired)
    PasswordResetError.TooManyAttempts -> UiText.Resource(R.string.error_reset_code_attempts)
    is PasswordResetError.TooManyRequests -> UiText.Resource(R.string.error_reset_too_many_requests, retryAfterSeconds)
    PasswordResetError.WeakPassword -> UiText.Resource(R.string.error_reset_weak_password)
    PasswordResetError.Unknown -> UiText.Resource(R.string.error_unknown)
}

/** Errores de lectura/escritura del perfil (spec 004). Se muestran en un Snackbar. */
fun DataError.toUiText(): UiText = UiText.Resource(
    when (this) {
        DataError.Network -> R.string.error_network
        DataError.NotFound -> R.string.error_profile_not_found
        DataError.FileTooLarge -> R.string.error_photo_too_large
        DataError.PermissionDenied, DataError.Unknown -> R.string.error_unknown
    },
)

/** Mensajes del cambio de correo con código (spec 004). `RequiresRecentLogin` abre la reautenticación: sin mensaje. */
fun EmailChangeError.toUiText(): UiText? = when (this) {
    EmailChangeError.RequiresRecentLogin -> null
    EmailChangeError.EmailAlreadyInUse -> UiText.Resource(R.string.error_email_in_use)
    EmailChangeError.InvalidEmail -> UiText.Resource(R.string.error_new_email_invalid)
    EmailChangeError.InvalidCode -> UiText.Resource(R.string.error_reset_code_invalid)
    EmailChangeError.ExpiredCode -> UiText.Resource(R.string.error_reset_code_expired)
    EmailChangeError.TooManyAttempts -> UiText.Resource(R.string.error_reset_code_attempts)
    is EmailChangeError.TooManyRequests -> UiText.Resource(R.string.error_reset_too_many_requests, retryAfterSeconds)
    EmailChangeError.Network -> UiText.Resource(R.string.error_network)
    EmailChangeError.Unknown -> UiText.Resource(R.string.error_unknown)
}
