package com.example.huellitasya.core.designsystem

import androidx.compose.ui.graphics.Color

/** Colores de una mascota en el calendario: [accent] para su nombre y anillos, [border] y [surface] para sus tarjetas. */
data class PetColors(val accent: Color, val border: Color, val surface: Color)

private val PetPalette = listOf(
    PetColors(accent = PetTealAccent, border = PetTealBorder, surface = PetTealSurface),
    PetColors(accent = PetOrangeAccent, border = PetOrangeAccent, surface = PetOrangeSurface),
    PetColors(accent = PetPinkAccent, border = PetPinkAccent, surface = PetPinkSurface),
)

/** Color fijo de la mascota en la posición [index] (orden de registro); con más de tres, se repiten. */
fun petColors(index: Int): PetColors = PetPalette[index.mod(PetPalette.size)]
