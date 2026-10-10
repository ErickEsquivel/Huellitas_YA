package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.ErrorRed
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.Nunito
import com.example.huellitasya.core.designsystem.WarningOrange

/**
 * Estado visual de un campo (Figma, "ERRORES LAYOUTS"):
 * - [Warning]: validación en cliente (dato faltante o con formato inválido), borde naranja.
 * - [Error]: el dato no coincide o el servidor lo rechazó, borde rojo.
 */
enum class FieldStatus { Default, Warning, Error }

private val FieldShape = RoundedCornerShape(10.dp)
private val LabelColor = Color.Black.copy(alpha = 0.5f)

/**
 * Campo de texto base de todos los formularios: etiqueta arriba, caja de 48 dp y,
 * opcionalmente, un mensaje con ícono debajo ([FieldMessage]).
 */
@Composable
fun HuellitasTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    status: FieldStatus = FieldStatus.Default,
    message: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        status == FieldStatus.Error -> ErrorRed
        status == FieldStatus.Warning -> WarningOrange
        isFocused -> GreenPrimary
        else -> BorderSubtle
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = LabelColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black,
            ),
            cursorBrush = SolidColor(GreenPrimary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, borderColor, FieldShape)
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                    trailingIcon?.invoke()
                }
            },
        )
        if (message != null && status != FieldStatus.Default) {
            Spacer(modifier = Modifier.height(6.dp))
            FieldMessage(text = message, status = status)
        }
    }
}

/** [HuellitasTextField] con contraseña oculta y botón para mostrarla. */
@Composable
fun HuellitasPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPasswordDescription: String,
    hidePasswordDescription: String,
    modifier: Modifier = Modifier,
    status: FieldStatus = FieldStatus.Default,
    message: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }

    HuellitasTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        status = status,
        message = message,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation('*'),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    painter = painterResource(
                        id = if (isVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                    ),
                    contentDescription = if (isVisible) hidePasswordDescription else showPasswordDescription,
                    tint = GrayText,
                )
            }
        },
    )
}

/**
 * Mensaje con ícono bajo un campo o un grupo de campos
 * (p. ej. "Rellene todos los datos para continuar").
 */
@Composable
fun FieldMessage(
    text: String,
    status: FieldStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status != FieldStatus.Default) {
            Image(
                painter = painterResource(
                    id = if (status == FieldStatus.Error) R.drawable.ic_field_error else R.drawable.ic_field_warning,
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = LabelColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HuellitasTextFieldPreview() {
    HuellitasYATheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HuellitasTextField(value = "Sa.company@hotmail.com", onValueChange = {}, label = "Correo")
            HuellitasTextField(
                value = "Sa.company@",
                onValueChange = {},
                label = "Correo",
                status = FieldStatus.Warning,
                message = "Ingresar un correo valido",
            )
            HuellitasPasswordField(
                value = "secreto123",
                onValueChange = {},
                label = "Contraseña",
                showPasswordDescription = "Mostrar contraseña",
                hidePasswordDescription = "Ocultar contraseña",
                status = FieldStatus.Error,
                message = "Correo o contraseña incorrectos",
            )
        }
    }
}
