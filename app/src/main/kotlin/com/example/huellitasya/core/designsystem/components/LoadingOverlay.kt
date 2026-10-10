package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Capa semitransparente que bloquea la interacción mientras hay una operación en curso. */
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.6f))
            // Consume los toques para que no lleguen a la pantalla de abajo.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = GreenPrimary)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingOverlayPreview() {
    HuellitasYATheme { LoadingOverlay(isVisible = true) }
}
