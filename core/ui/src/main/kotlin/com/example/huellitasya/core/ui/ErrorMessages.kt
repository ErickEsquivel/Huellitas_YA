package com.example.huellitasya.core.ui

import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
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

/** Mensajes de la spec 002 para cada error de validación. */
fun ValidationError.toUiText(): UiText = UiText.Resource(
    when (this) {
        ValidationError.EmptyName -> R.string.error_empty_name
        ValidationError.NameTooShort -> R.string.error_name_too_short
        ValidationError.EmptyEmail -> R.string.error_empty_email
        ValidationError.InvalidEmail -> R.string.error_invalid_email
        ValidationError.EmptyPassword -> R.string.error_empty_password
        ValidationError.PasswordTooShort -> R.string.error_password_too_short
        ValidationError.PasswordNeedsUppercase -> R.string.error_password_uppercase
        ValidationError.PasswordNeedsDigitOrSymbol -> R.string.error_password_digit_or_symbol
        ValidationError.PasswordsDoNotMatch -> R.string.error_passwords_do_not_match
        ValidationError.TermsNotAccepted -> R.string.error_terms_not_accepted
    },
)

/** Mensajes de la spec 002 para errores remotos. `Cancelled` no tiene mensaje: devuelve `null`. */
fun AuthError.toUiText(): UiText? = when (this) {
    AuthError.Cancelled -> null
    AuthError.InvalidCredentials -> UiText.Resource(R.string.error_invalid_credentials)
    AuthError.EmailAlreadyInUse -> UiText.Resource(R.string.error_email_in_use)
    AuthError.WeakPassword -> UiText.Resource(R.string.error_weak_password)
    AuthError.Network -> UiText.Resource(R.string.error_network)
    AuthError.TooManyRequests -> UiText.Resource(R.string.error_too_many_requests)
    AuthError.GoogleSignInFailed -> UiText.Resource(R.string.error_google_sign_in)
    AuthError.Unknown -> UiText.Resource(R.string.error_unknown)
}
