package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Flecha "<" de Figma (16×14 dp) arriba a la izquierda de los formularios y de Datos Personales. */
@Composable
fun HuellitasBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_back_chevron),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .requiredSize(width = 16.dp, height = 14.dp)
                .rotate(90f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HuellitasBackButtonPreview() {
    HuellitasYATheme { HuellitasBackButton(onClick = {}, contentDescription = "Volver") }
}
