package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.core.designsystem.ErrorRed
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.WarningOrange

private val BoxShape = RoundedCornerShape(10.dp)
private val BoxBorder = Color.Black.copy(alpha = 0.4f)

/**
 * Código numérico en casillas (Figma "Verifica tu correo": 50x64 dp, 34 dp de separación).
 * Es un único campo de texto: se puede escribir dígito a dígito o pegar el código completo.
 * La casilla que recibe el siguiente dígito se resalta en verde.
 */
@Composable
fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    status: FieldStatus = FieldStatus.Default,
    enabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(length)) },
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interactionSource,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                repeat(length) { index ->
                    val isNext = isFocused && index == value.length.coerceAtMost(length - 1)
                    val borderColor = when {
                        status == FieldStatus.Error -> ErrorRed
                        status == FieldStatus.Warning -> WarningOrange
                        isNext -> GreenPrimary
                        else -> BoxBorder
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp, height = 64.dp)
                            .border(width = if (isNext) 2.dp else 1.dp, color = borderColor, shape = BoxShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = value.getOrNull(index)?.toString().orEmpty(),
                            color = Color.Black,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun OtpCodeFieldPreview() {
    HuellitasYATheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OtpCodeField(value = "", onValueChange = {}, length = 4, contentDescription = "Código")
            OtpCodeField(value = "21", onValueChange = {}, length = 4, contentDescription = "Código", status = FieldStatus.Warning)
            OtpCodeField(value = "2145", onValueChange = {}, length = 4, contentDescription = "Código", status = FieldStatus.Error)
        }
    }
}
