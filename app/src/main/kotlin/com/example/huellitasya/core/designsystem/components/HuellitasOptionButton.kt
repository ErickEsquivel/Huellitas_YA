package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.MintSurface

private val OptionShape = RoundedCornerShape(50)

/**
 * Botón de opción con ícono a la izquierda (Figma "BIENVENIDO": "Continuar con correo",
 * "Continuar con Google", "Regístrate"). Mide 319x46 dp por defecto.
 * El ícono ocupa una columna fija para que los textos de varios botones queden alineados.
 */
@Composable
fun HuellitasOptionButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: DpSize = DpSize(28.dp, 28.dp),
    containerColor: Color = GreenAction,
    border: BorderStroke = BorderStroke(1.dp, BorderSubtle),
    contentColor: Color = Color.White,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    enabled: Boolean = true,
    iconColumnStart: Dp = 38.dp,
    textStart: Dp = 87.dp,
) {
    Box(
        modifier = modifier
            .width(319.dp)
            .height(46.dp)
            .clip(OptionShape)
            .background(containerColor)
            .border(border, OptionShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = iconColumnStart)
                .width(textStart - iconColumnStart),
            contentAlignment = Alignment.CenterStart,
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .width(iconSize.width)
                    .height(iconSize.height),
            )
        }
        Row(
            modifier = Modifier.padding(start = textStart, end = 16.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(text = text, color = contentColor, fontSize = 16.sp, fontWeight = fontWeight)
        }
    }
}

/** Variante "Continuar con Google": fondo blanco, borde verde y logo de Google. */
@Composable
fun GoogleOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    HuellitasOptionButton(
        text = text,
        icon = painterResource(R.drawable.ic_google),
        onClick = onClick,
        modifier = modifier,
        iconSize = DpSize(34.dp, 34.dp),
        containerColor = Color.White.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, GreenPrimary),
        contentColor = Color.Black,
        fontWeight = FontWeight.Bold,
        enabled = enabled,
        iconColumnStart = 39.dp,
    )
}

@Preview(showBackground = true)
@Composable
private fun HuellitasOptionButtonPreview() {
    HuellitasYATheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            GoogleOptionButton(text = "Continuar con Google", onClick = {})
            HuellitasOptionButton(
                text = "Regístrate",
                icon = painterResource(R.drawable.ic_add_placeholder),
                onClick = {},
                containerColor = MintSurface.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f)),
                contentColor = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
