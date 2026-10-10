package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.R
import com.example.huellitasya.core.designsystem.SurfaceGray

/** Tarjeta gris con un "+" para agregar un elemento (mascota, evento...). Mide 367x87 dp por defecto. */
@Composable
fun AddPlaceholderCard(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(367.dp)
            .height(87.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceGray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_add_placeholder),
            contentDescription = contentDescription,
            modifier = Modifier.size(50.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddPlaceholderCardPreview() {
    HuellitasYATheme { AddPlaceholderCard(contentDescription = "Agregar mascota", onClick = {}) }
}
