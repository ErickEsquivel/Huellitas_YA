package com.example.huellitasya.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Fuente de Figma (Nunito). Licencia SIL Open Font License 1.1. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val Default = Typography()

private fun TextStyle.nunito() = copy(fontFamily = Nunito)

// Todos los estilos usan Nunito: cualquier `Text` hereda la fuente de Figma aunque defina su propio tamaño/peso.
val Typography = Typography(
    displayLarge = Default.displayLarge.nunito(),
    displayMedium = Default.displayMedium.nunito(),
    displaySmall = Default.displaySmall.nunito(),
    headlineLarge = Default.headlineLarge.nunito(),
    headlineMedium = Default.headlineMedium.nunito(),
    headlineSmall = Default.headlineSmall.nunito(),
    titleLarge = Default.titleLarge.nunito(),
    titleMedium = Default.titleMedium.nunito(),
    titleSmall = Default.titleSmall.nunito(),
    bodyLarge = Default.bodyLarge.nunito(),
    bodyMedium = Default.bodyMedium.nunito(),
    bodySmall = Default.bodySmall.nunito(),
    labelLarge = Default.labelLarge.nunito(),
    labelMedium = Default.labelMedium.nunito(),
    labelSmall = Default.labelSmall.nunito(),
)
