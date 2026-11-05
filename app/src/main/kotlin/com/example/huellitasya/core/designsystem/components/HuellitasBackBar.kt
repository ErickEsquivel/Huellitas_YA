package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Alto de [HuellitasBackBar]: el contenido con scroll deja este margen arriba para no pasar por debajo. */
val HuellitasBackBarHeight = 48.dp

/**
 * Flecha de volver, en el mismo lugar en todas las pantallas. Va fuera del contenido con scroll,
 * como hermana de este dentro del mismo `Box`, para que siga visible al desplazar:
 *
 * ```
 * Box {
 *     HuellitasBackBar(onClick = onBackClick, contentDescription = ...)
 *     Column(Modifier.fillMaxSize().padding(top = HuellitasBackBarHeight).verticalScroll(...)) { ... }
 * }
 * ```
 *
 * No suma la barra de estado: el `Scaffold` de `HuellitasApp` ya la descuenta. Auth dibuja sobre
 * la foto y le pasa `Modifier.statusBarsPadding()`.
 */
@Composable
fun HuellitasBackBar(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HuellitasBackBarHeight)
            .padding(start = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_back_arrow),
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HuellitasBackBarPreview() {
    HuellitasYATheme { HuellitasBackBar(onClick = {}, contentDescription = "Volver") }
}
