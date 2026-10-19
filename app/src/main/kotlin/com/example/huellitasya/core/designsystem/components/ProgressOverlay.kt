package com.example.huellitasya.core.designsystem.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenDeep
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.OverlayScrim

/**
 * Dibuja [content] y, mientras [isVisible], lo desenfoca (API 31+) y lo cubre con el spinner de
 * Figma y [text] ("Guardando...", "Actualizando...", "Eliminando..."; spec 005). Bloquea los toques.
 */
@Composable
fun ProgressOverlay(
    isVisible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(7.5.dp) else Modifier
                )
        ) {
            content()
        }
        AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OverlayScrim)
                    // Consume los toques para que no lleguen a la pantalla de abajo.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SpinnerWithLabel(text = text)
            }
        }
    }
}

/** Spinner de Figma (3 vueltas lineales en 2.4 s y una pausa breve) con un texto verde debajo. */
@Composable
fun SpinnerWithLabel(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1080f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0 using LinearEasing
                360f at 800 using LinearEasing
                720f at 1600 using LinearEasing
                1080f at 2400 using LinearEasing
                1080f at 2500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinnerRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_spinner_track),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(R.drawable.ic_spinner_arc),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = text,
            color = GreenDeep,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ProgressOverlayPreview() {
    HuellitasYATheme {
        ProgressOverlay(isVisible = true, text = "Guardando...") {
            Text(text = "Contenido de la pantalla", modifier = Modifier.size(200.dp))
        }
    }
}
