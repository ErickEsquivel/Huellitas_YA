package com.example.huellitasya.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.ScreenHeader

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        isLoggingOut = uiState.isLoggingOut,
        onLogoutClick = viewModel::logout,
    )
}

@Composable
internal fun SettingsScreen(
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // Fondo curvado
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.3f)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            ScreenHeader(
                title = "Configuración",
                titleFontSize = 32.sp,
                notificationsDescription = "Notificaciones",
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Título "Mi cuenta"
            Text(
                text = "Mi cuenta",
                color = GrayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, // 800
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta 1: Datos personales
            SettingsCard(
                title = "Datos personales",
                subtitle = "Nombre, correo y mas",
                iconRes = R.drawable.ic_settings_personal,
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta 2: Seguridad
            SettingsCard(
                title = "Seguridad",
                subtitle = "Cambiar contraseña",
                iconRes = R.drawable.ic_settings_security,
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Botón Cerrar Sesión
            HuellitasButton(
                text = "Cerrar sesión",
                onClick = onLogoutClick,
                isLoading = isLoggingOut,
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// Subcomponente para las tarjetas de configuración
@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(360.dp)
            .height(62.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícono izquierdo
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Textos (Título y subtítulo)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold // 700
            )
            Text(
                text = subtitle,
                color = GrayText, // El color del subtítulo es gris en la imagen
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold // 800
            )
        }

        // Ícono de flecha derecha (Chevron)
        Text(
            text = ">",
            color = GrayText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    HuellitasYATheme { SettingsScreen(isLoggingOut = false, onLogoutClick = {}) }
}
