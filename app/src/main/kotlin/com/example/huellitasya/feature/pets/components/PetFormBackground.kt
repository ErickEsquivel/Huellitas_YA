package com.example.huellitasya.feature.pets.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.huellitasya.core.designsystem.GreenBackground

/**
 * Ola verde detrás de los campos del formulario de mascota (Figma 19:63). Va dentro del contenido
 * que se desplaza, así siempre empieza bajo la foto, y se prolonga hacia abajo para cubrir hasta
 * el borde de la pantalla aunque el formulario sea más corto.
 *
 * @param horizontalBleed padding horizontal del contenedor: la ola lo cubre de borde a borde.
 */
internal fun Modifier.petFormWave(horizontalBleed: Dp): Modifier = drawBehind {
    val bleed = horizontalBleed.toPx()
    val left = -bleed
    val right = size.width + bleed
    val width = right - left
    val bottom = size.height + EXTRA_HEIGHT.toPx()
    val path = Path().apply {
        moveTo(left, 34.dp.toPx())
        quadraticTo(left + width * 0.5f, (-26).dp.toPx(), right, 40.dp.toPx())
        lineTo(right, bottom)
        lineTo(left, bottom)
        close()
    }
    drawPath(path, color = GreenBackground)
}

/** Lo que la ola sigue hacia abajo del formulario: más que cualquier alto de pantalla. */
private val EXTRA_HEIGHT = 2000.dp
