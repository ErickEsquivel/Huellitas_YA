package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import com.example.huellitasya.core.designsystem.GreenBackground

/**
 * Dibuja un fondo con forma de ola u ondulado.
 *
 * @param modifier El modificador para ajustar el tamaño o la alineación (ej: fillMaxHeight(0.5f), align(BottomCenter))
 */
@Composable
fun CurvedBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth()) {
        val path = Path().apply {
            // Empezamos en la izquierda, un poco por debajo del tope del Canvas (20%)
            moveTo(0f, size.height * 0.2f)
            
            // Hacemos una curva cuadrática de Bézier hacia arriba en el medio
            quadraticTo(
                size.width * 0.5f, -size.height * 0.1f, // Puntos de control
                size.width, size.height * 0.3f // Puntos de destino
            )
            
            // Cerramos el trazo formando un rectángulo desde la ola hacia abajo
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color = GreenBackground)
    }
}
