package com.example.huellitasya.core.ui

/** Estado del diálogo "Confirma tu contraseña" (`PasswordConfirmDialog`) que reautentica antes de una operación sensible. */
data class PasswordPromptState(
    val password: String = "",
    val feedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
)
