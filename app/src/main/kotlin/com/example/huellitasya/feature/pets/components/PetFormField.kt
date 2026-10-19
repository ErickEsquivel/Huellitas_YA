package com.example.huellitasya.feature.pets.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.ErrorRed
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.WarningOrange
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus

private val FieldShape = RoundedCornerShape(10.dp)
private val LabelColor = Color.Black.copy(alpha = 0.2f)
private val ValueStyle = TextStyle(color = Color.Black, fontSize = 19.sp, fontWeight = FontWeight.Bold)

/** Distancia de Figma entre el borde de la tarjeta y la etiqueta/valor (93 − 24 dp). */
private val TextStart = 69.dp

/**
 * Tarjeta de un campo del formulario de mascota (Figma 19:63): ícono a la izquierda, etiqueta
 * gris arriba y el valor debajo. Con [status] distinto de `Default` el borde se colorea y, si
 * hay [message], se muestra debajo.
 */
@Composable
private fun PetFieldCard(
    label: String,
    icon: Painter,
    status: FieldStatus,
    message: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 50.dp,
    cardModifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Box(
            modifier = cardModifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .background(Color.White, FieldShape)
                .border(1.dp, status.borderColor, FieldShape),
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (TextStart - iconSize) / 2)
                    .size(iconSize),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TextStart, end = 12.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Text(text = label, color = LabelColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                value()
            }
        }
        message?.let {
            FieldMessage(text = it, status = status, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private val FieldStatus.borderColor: Color
    get() = when (this) {
        FieldStatus.Default -> BorderSubtle
        FieldStatus.Warning -> WarningOrange
        FieldStatus.Error -> ErrorRed
    }

/** Campo de texto (Nombre, Raza, Peso). [suffix] se muestra tras el valor ("KG"). */
@Composable
internal fun PetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    status: FieldStatus = FieldStatus.Default,
    message: String? = null,
    iconSize: Dp = 50.dp,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    PetFieldCard(
        label = label,
        icon = icon,
        status = status,
        message = message,
        modifier = modifier,
        iconSize = iconSize,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = ValueStyle,
            cursorBrush = SolidColor(GreenPrimary),
            keyboardOptions = keyboardOptions,
            visualTransformation = suffix?.let(::SuffixTransformation) ?: VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Muestra " [suffix]" tras el valor sin que forme parte del texto ("5" → "5 KG"). */
private class SuffixTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val length = text.length
        return TransformedText(
            AnnotatedString(text.text + " " + suffix),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset
                override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(length)
            },
        )
    }
}

/** Campo que se elige tocándolo (Fecha de nacimiento): muestra [valueText] o nada. */
@Composable
internal fun PetClickableField(
    valueText: String?,
    onClick: () -> Unit,
    label: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    status: FieldStatus = FieldStatus.Default,
    message: String? = null,
    iconSize: Dp = 50.dp,
) {
    PetFieldCard(
        label = label,
        icon = icon,
        status = status,
        message = message,
        modifier = modifier,
        iconSize = iconSize,
        cardModifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(text = valueText.orEmpty(), style = ValueStyle, maxLines = 1)
    }
}

/** Campo con un menú de opciones (Especie, Sexo). */
@Composable
internal fun <T> PetDropdownField(
    selected: T?,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    label: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    status: FieldStatus = FieldStatus.Default,
    message: String? = null,
    iconSize: Dp = 50.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        PetClickableField(
            valueText = selected?.let { optionLabel(it) },
            onClick = { expanded = true },
            label = label,
            icon = icon,
            status = status,
            message = message,
            iconSize = iconSize,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = optionLabel(option), style = ValueStyle.copy(fontSize = 17.sp)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
