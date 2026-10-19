package com.example.huellitasya.feature.auth.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasSpacing
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LightGrayText
import com.example.huellitasya.core.designsystem.components.AppTitle
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.HuellitasButton

@Composable
internal fun WelcomeScreen(onStartClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Fondo verde curvado (ola) en la mitad inferior
        CurvedBackground(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.55f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // La app dibuja edge-to-edge: el pie no debe quedar debajo de la barra de navegación.
                .navigationBarsPadding()
                .padding(horizontal = HuellitasSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(HuellitasSpacing.xxl))

            AppTitle(logoDescription = stringResource(R.string.auth_logo_description))

            Spacer(modifier = Modifier.height(HuellitasSpacing.sm))

            Text(
                text = stringResource(R.string.auth_welcome_slogan),
                color = GrayText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(HuellitasSpacing.md))

            Text(
                text = stringResource(R.string.auth_welcome_description),
                color = LightGrayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // 386 dp como en Figma; en pantallas bajas la foto se encoge para que el botón y el pie
            // queden encima de la barra de navegación.
            Image(
                painter = painterResource(id = R.drawable.img_pets),
                contentDescription = stringResource(R.string.auth_welcome_pets_description),
                modifier = Modifier
                    .weight(12f, fill = false)
                    .widthIn(max = 386.dp)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.weight(1f))

            HuellitasButton(
                text = stringResource(R.string.auth_welcome_start),
                onClick = onStartClick
            )

            Spacer(modifier = Modifier.height(HuellitasSpacing.md))

            Text(
                text = stringResource(R.string.auth_welcome_footer),
                color = GrayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(HuellitasSpacing.xl))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    HuellitasYATheme {
        WelcomeScreen(onStartClick = {})
    }
}
