package com.example.huellitasya.core.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Enlace de texto, p. ej. "¿Olvidaste tu contraseña?" o "Crear cuenta". */
@Composable
fun HuellitasTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = GreenPrimary,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HuellitasTextButtonPreview() {
    HuellitasYATheme { HuellitasTextButton(text = "¿Olvidaste tu contraseña?", onClick = {}) }
}
