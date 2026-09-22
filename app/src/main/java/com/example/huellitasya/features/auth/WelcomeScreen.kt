package com.example.huellitasya.features.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.*
import com.example.huellitasya.core.designsystem.components.HuellitasButton

@Composable
fun WelcomeScreen(onStartClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Fondo verde curvado (ola irregular) en la parte inferior
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.55f) // Cubre un poco más de la mitad inferior de la pantalla
        ) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.2f)
                // Usamos Curvas Bezier Cuadráticas para dar ese efecto orgánico del diseño
                quadraticBezierTo(
                    size.width * 0.5f, -size.height * 0.1f, // Curva hacia arriba
                    size.width, size.height * 0.3f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, color = GreenBackground)
        }

        // Contenido por encima del fondo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo_huellitas),
                contentDescription = "Logo Huellitas YA!",
                modifier = Modifier.size(width = 126.dp, height = 108.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Titulo "Huellitas YA!"
            Text(
                text = buildAnnotatedString {
                    append("Huellitas ")
                    withStyle(style = SpanStyle(color = GreenPrimary)) {
                        append("YA!")
                    }
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold, // 800
                color = Color.Black
                // TODO: Cuando configures la fuente de Figma en Type.kt, puedes agregar: fontFamily = Nunito
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Eslogan
            Text(
                text = "El cuidado de tu mascota, en\nun solo lugar. \uD83D\uDC3E", // \uD83D\uDC3E es el emoji de huellitas
                color = GrayText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold, // 700
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Texto intermedio
            Text(
                text = "Organiza vacunas, historial médico y\ngestiona la frecuencia de vacunación\nde tu mascota.",
                color = LightGrayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold, // 700
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Imagen Mascotas
            Image(
                painter = painterResource(id = R.drawable.img_pets),
                contentDescription = "Perro y gato",
                modifier = Modifier.size(386.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Botón
            HuellitasButton(
                text = "Comenzar",
                onClick = onStartClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frase final
            Text(
                text = "Tu mascota merece el mejor cuidado \uD83D\uDC9A", // \uD83D\uDC9A es el emoji de corazón verde
                color = GrayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, // 800
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    HuellitasYATheme {
        WelcomeScreen()
    }
}
