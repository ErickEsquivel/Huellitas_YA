package com.example.huellitasya.feature.pets.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.huellitasya.feature.pets.intro.PetsIntroScreen
import com.example.huellitasya.feature.pets.register.RegisterPetRoute
import kotlinx.serialization.Serializable

/** Presentación tras el login, si la cuenta no tiene mascotas (spec 005). */
@Serializable
data object PetsIntroDestination

fun NavGraphBuilder.petsIntroScreen(onStartClick: () -> Unit) {
    composable<PetsIntroDestination> {
        PetsIntroScreen(onStartClick = onStartClick)
    }
}

/**
 * "¡Conozcamos a tu mascota!" (spec 005).
 *
 * @param fromIntro `true` si se abrió desde Presentación: al guardar, Home reemplaza a ambas.
 */
@Serializable
data class RegisterPetDestination(val fromIntro: Boolean = false)

/** @param onPetSaved recibe `fromIntro` para decidir cómo volver a Home. */
fun NavGraphBuilder.registerPetScreen(
    onBack: () -> Unit,
    onPetSaved: (fromIntro: Boolean) -> Unit,
) {
    composable<RegisterPetDestination> { entry ->
        val destination = entry.toRoute<RegisterPetDestination>()
        RegisterPetRoute(
            onBackClick = onBack,
            onPetSaved = { onPetSaved(destination.fromIntro) },
        )
    }
}
