package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasSpacing
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/**
 * Encabezado de las pantallas principales: título, subtítulo opcional y campana de notificaciones.
 *
 * @param titleFontSize 20sp en Home/Calendario/Perfil; Configuración usa un título más grande.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleFontSize: TextUnit = 20.sp,
    notificationsDescription: String? = null,
    onNotificationsClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(HuellitasSpacing.xs))
                Text(
                    text = subtitle,
                    color = GrayText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_bell_placeholder),
            contentDescription = notificationsDescription,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onNotificationsClick),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenHeaderPreview() {
    HuellitasYATheme {
        ScreenHeader(title = "Calendario", subtitle = "Gestiona los eventos de tu mascota")
    }
}
