package com.example.huellitasya.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.ErrorRed
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LightGrayText
import com.example.huellitasya.core.designsystem.Nunito
import com.example.huellitasya.core.designsystem.WarningOrange
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus

private val ValueStyle = TextStyle(
    fontFamily = Nunito,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    color = Color.Black,
)

/**
 * Campo subrayado de la tarjeta "Datos Dueño" (Figma 3254:1023): etiqueta gris de 14 sp, valor
 * de 16 sp y una línea debajo. En modo lectura muestra el texto; en modo edición se puede escribir.
 *
 * @param placeholder texto en gris cuando el valor está vacío (p. ej. "Sin registrar").
 * @param onClick si se indica, el valor no se escribe: el campo abre un selector (fecha de nacimiento).
 */
@Composable
internal fun PersonalDataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    editable: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    status: FieldStatus = FieldStatus.Default,
    message: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val lineColor = when {
        status == FieldStatus.Error -> ErrorRed
        status == FieldStatus.Warning -> WarningOrange
        editable && isFocused -> GreenPrimary
        else -> BorderSubtle
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = GrayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 26.dp)
                .then(
                    if (editable && onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (editable && onClick == null) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = ValueStyle,
                    cursorBrush = SolidColor(GreenPrimary),
                    keyboardOptions = keyboardOptions,
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = value.ifEmpty { placeholder.orEmpty() },
                    style = ValueStyle,
                    color = if (value.isEmpty()) LightGrayText else Color.Black,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor),
        )
        if (message != null && status != FieldStatus.Default) {
            FieldMessage(
                text = message,
                status = status,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PersonalDataFieldPreview() {
    HuellitasYATheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PersonalDataField(label = "Nombre completo", value = "Santiago Cruz", onValueChange = {}, editable = false)
            PersonalDataField(
                label = "Teléfono",
                value = "",
                onValueChange = {},
                editable = false,
                placeholder = "Sin registrar",
            )
            PersonalDataField(
                label = "Teléfono",
                value = "31244",
                onValueChange = {},
                editable = true,
                status = FieldStatus.Warning,
                message = "Ingresa un teléfono válido",
            )
        }
    }
}
