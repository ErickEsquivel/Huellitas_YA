package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.AlertRed
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.OverlayScrim

// Medidas de la tarjeta en Figma (3050:1584), sobre un frame de 412 dp de ancho.
private const val CardWidth = 393.65f
private const val CardHeight = 455f
private const val TitleTop = 212f
private const val ButtonWidth = 237.72f
private const val ButtonHeight = 40.9f

/**
 * "¿Seguro deseas eliminarlo?" (Figma `EDIT PERRO 2`, 3050:1515): eliminar una mascota (spec 005)
 * y eliminar la cuenta (spec 004). La tarjeta con el cachorro y la papelera es una sola imagen; los
 * textos y el botón van encima en las posiciones de Figma, escaladas al ancho disponible.
 *
 * @param confirmText "Eliminar Información" o "Eliminar cuenta".
 */
@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    closeDescription: String,
    imageDescription: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        DeleteConfirmDialogContent(title, message, confirmText, closeDescription, imageDescription, onConfirm, onDismiss)
    }
}

@Composable
private fun DeleteConfirmDialogContent(
    title: String,
    message: String,
    confirmText: String,
    closeDescription: String,
    imageDescription: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayScrim)
            // Tocar fuera de la tarjeta cierra el diálogo, como la X.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 4.dp, end = 8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_dialog_close),
                contentDescription = closeDescription,
                modifier = Modifier.size(37.5.dp),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 9.dp),
        ) {
            // Tarjeta y textos se escalan juntos; Figma mide 393.65 dp y un teléfono de 384 dp no la deja entera.
            val scale = minOf(1f, maxWidth.value / CardWidth)
            Box(
                modifier = Modifier
                    .size(width = (CardWidth * scale).dp, height = (CardHeight * scale).dp)
                    // La tarjeta no cierra el diálogo al tocarla.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.img_delete_dialog),
                    contentDescription = imageDescription,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height((TitleTop * scale).dp))
                    // La zona blanca de la tarjeta es más angosta que la imagen: el título se reduce si no cabe.
                    Text(
                        text = title,
                        color = AlertRed,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = (26 * scale).sp),
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = (32 * scale).dp),
                    )
                    Spacer(modifier = Modifier.height((16 * scale).dp))
                    Text(
                        text = message,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontSize = (16 * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width((253 * scale).dp),
                    )
                    Spacer(modifier = Modifier.height((31 * scale).dp))
                    val buttonShape = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .size(width = (ButtonWidth * scale).dp, height = (ButtonHeight * scale).dp)
                            .background(GreenPrimary, buttonShape)
                            .border(1.dp, BorderSubtle, buttonShape)
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = confirmText,
                            color = Color.White,
                            fontSize = (20 * scale).sp,
                            maxLines = 1,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun DeleteConfirmDialogPreview() {
    HuellitasYATheme {
        DeleteConfirmDialogContent(
            title = "¿Seguro deseas eliminarlo?",
            message = "No podrás recuperar los datos luego de confirmar la eliminación.",
            confirmText = "Eliminar Información",
            closeDescription = "Cerrar",
            imageDescription = "Cachorro triste junto a una papelera",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
