package com.example.huellitasya.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.huellitasya.core.designsystem.HuellitasSpacing
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.AddPlaceholderCard
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.ScreenHeader

@Composable
fun HomeScreen(
    onAddPetClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.55f)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = HuellitasSpacing.lg)
        ) {
            Spacer(modifier = Modifier.height(HuellitasSpacing.xxl))

            ScreenHeader(
                title = "Hola, cómo estás?!",
                subtitle = "Veamos como están tus mascotas",
                notificationsDescription = "Notificaciones",
            )

            Spacer(modifier = Modifier.height(HuellitasSpacing.xl))

            AddPlaceholderCard(
                contentDescription = "Agregar Mascota",
                onClick = onAddPetClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HuellitasYATheme { HomeScreen() }
}
