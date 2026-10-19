package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenHighlight
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Logo + "Huellitas YA!" con el "YA!" en verde. Lo usan Bienvenido (spec 002) y Presentación (spec 005). */
@Composable
fun AppTitle(
    logoDescription: String,
    modifier: Modifier = Modifier,
    logoSize: DpSize = DpSize(126.dp, 108.dp),
    spacing: Dp = 16.dp,
    fontSize: TextUnit = 48.sp,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_huellitas),
            contentDescription = logoDescription,
            modifier = Modifier.size(logoSize)
        )
        Spacer(modifier = Modifier.height(spacing))
        Text(
            text = buildAnnotatedString {
                append("Huellitas ")
                withStyle(SpanStyle(color = GreenHighlight)) { append("YA!") }
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTitlePreview() {
    HuellitasYATheme { AppTitle(logoDescription = "Logo Huellitas YA!") }
}
