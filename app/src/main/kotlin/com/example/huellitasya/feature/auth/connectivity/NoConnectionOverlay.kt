package com.example.huellitasya.feature.auth.connectivity

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.AlertRed
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.OverlayScrim
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.SpinnerWithLabel

/** Ancho y alto de la tarjeta ilustrada en Figma (frame de 412 dp). */
private const val CARD_DESIGN_WIDTH = 412f
private const val CARD_DESIGN_HEIGHT = 477f

/**
 * Dibuja [content] (una pantalla de auth) y, encima, el overlay "Sin conexión" cuando
 * [viewModel] lo indica. Con el overlay visible, la pantalla de fondo se desenfoca (API 31+).
 */
@Composable
internal fun ConnectivityAware(
    viewModel: ConnectivityViewModel,
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOverlayVisible = state != ConnectionUiState.Online

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isOverlayVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(7.5.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
        NoConnectionOverlay(state = state, onRetry = viewModel::onRetry)
    }
}

/** Overlay a pantalla completa de Figma "ERROR INTERNET Overlay" (159:647), estados "error" y "loading". */
@Composable
internal fun NoConnectionOverlay(
    state: ConnectionUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Se recuerda el último estado visible para que la animación de salida no cambie de contenido.
    var shownState by remember { mutableStateOf(ConnectionUiState.Offline) }
    if (state != ConnectionUiState.Online) shownState = state

    AnimatedVisibility(
        visible = state != ConnectionUiState.Online,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
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
        ) {
            when (shownState) {
                ConnectionUiState.Reconnecting -> SpinnerWithLabel(
                    text = stringResource(R.string.auth_offline_connecting),
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> NoConnectionCard(onRetry = onRetry)
            }
        }
    }
}

/** Estado "error": tarjeta con el cachorro, título, mensaje y "Reintentar". */
@Composable
private fun NoConnectionCard(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // En Figma la tarjeta empieza a 120 dp y quedan 320 dp debajo: se respeta esa proporción.
        Spacer(modifier = Modifier.weight(120f))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_DESIGN_WIDTH / CARD_DESIGN_HEIGHT)
        ) {
            val scale = maxWidth.value / CARD_DESIGN_WIDTH
            Image(
                painter = painterResource(R.drawable.img_no_connection),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = stringResource(R.string.auth_offline_title),
                color = AlertRed,
                fontSize = (32 * scale.coerceAtMost(1f)).sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (217 * scale).dp)
            )
            Text(
                text = stringResource(R.string.auth_offline_message),
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (276 * scale).dp)
                    .width((297 * scale).dp)
            )
            HuellitasButton(
                text = stringResource(R.string.auth_offline_retry),
                onClick = onRetry,
                containerColor = GreenAction,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (386 * scale).dp)
                    .width((319 * scale).dp)
            )
        }
        Spacer(modifier = Modifier.weight(320f))
    }
}

@Preview(name = "Sin conexión", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NoConnectionOverlayPreview() {
    HuellitasYATheme {
        NoConnectionOverlay(state = ConnectionUiState.Offline, onRetry = {})
    }
}

@Preview(name = "Conectando", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ConnectingOverlayPreview() {
    HuellitasYATheme {
        NoConnectionOverlay(state = ConnectionUiState.Reconnecting, onRetry = {})
    }
}
