package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/**
 * Pide la contraseña antes de una operación sensible (cambiar el correo, eliminar la cuenta).
 * Los textos llegan por parámetro; el mensaje bajo el campo sigue los estados de [FieldStatus].
 */
@Composable
fun PasswordConfirmDialog(
    title: String,
    message: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordLabel: String,
    showPasswordDescription: String,
    hidePasswordDescription: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    status: FieldStatus = FieldStatus.Default,
    statusMessage: String? = null,
    isLoading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = Color.White,
        title = {
            Text(text = title, color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column {
                Text(text = message, color = GrayText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                HuellitasPasswordField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = passwordLabel,
                    showPasswordDescription = showPasswordDescription,
                    hidePasswordDescription = hidePasswordDescription,
                    status = status,
                    message = statusMessage,
                    enabled = !isLoading,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    compact = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                } else {
                    Text(text = confirmText, color = GreenPrimary, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(text = dismissText, color = GrayText, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Preview
@Composable
private fun PasswordConfirmDialogPreview() {
    HuellitasYATheme {
        PasswordConfirmDialog(
            title = "Confirma tu contraseña",
            message = "Por seguridad, ingresa tu contraseña para continuar.",
            password = "secreto",
            onPasswordChange = {},
            passwordLabel = "Contraseña",
            showPasswordDescription = "Mostrar contraseña",
            hidePasswordDescription = "Ocultar contraseña",
            confirmText = "Confirmar",
            dismissText = "Cancelar",
            onConfirm = {},
            onDismiss = {},
            status = FieldStatus.Error,
            statusMessage = "Correo o contraseña incorrectos",
        )
    }
}
