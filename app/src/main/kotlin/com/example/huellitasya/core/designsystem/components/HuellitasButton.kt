package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/**
 * Botón principal de la app. Mide 273x46 dp por defecto (Figma); el llamador puede
 * sobreescribir el ancho, p. ej. con `Modifier.width(319.dp)` o `fillMaxWidth()`.
 *
 * @param containerColor los formularios usan [GreenAction] cuando están completos y
 *   [ButtonIncomplete] mientras faltan datos (el botón sigue siendo pulsable para mostrar avisos).
 * @param isLoading muestra un indicador en lugar del texto y bloquea nuevos clics.
 */
@Composable
fun HuellitasButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = GreenPrimary,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(273.dp)
            .height(46.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            disabledContentColor = Color.White,
        ),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, BorderSubtle),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = fontWeight,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HuellitasButtonPreview() {
    HuellitasYATheme {
        Column {
            HuellitasButton(text = "Comenzar", onClick = {})
            HuellitasButton(text = "Iniciar sesión", onClick = {}, containerColor = GreenAction)
            HuellitasButton(text = "Iniciar sesión", onClick = {}, containerColor = ButtonIncomplete)
            HuellitasButton(text = "Iniciar sesión", onClick = {}, containerColor = GreenAction, isLoading = true)
        }
    }
}
