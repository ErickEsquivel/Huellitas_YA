package com.example.huellitasya.feature.pets.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.huellitasya.feature.pets.detail.PetDetailRoute
import com.example.huellitasya.feature.pets.edit.EditPetRoute
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

/** Perfil de una mascota. El nombre de la propiedad es la clave que lee `PetDetailViewModel`. */
@Serializable
data class PetDetailDestination(val petId: String)

/** @param onPetGone la mascota se eliminó: vuelve a Home. */
fun NavGraphBuilder.petDetailScreen(
    onBack: () -> Unit,
    onEditClick: (petId: String) -> Unit,
    onPetGone: () -> Unit,
) {
    composable<PetDetailDestination> {
        PetDetailRoute(onBackClick = onBack, onEditClick = onEditClick, onPetGone = onPetGone)
    }
}

/** Editar mascota. El nombre de la propiedad es la clave que lee `EditPetViewModel`. */
@Serializable
data class EditPetDestination(val petId: String)

/**
 * @param onPetUpdated vuelve al perfil, que ya muestra los cambios en vivo.
 * @param onPetDeleted vuelve a Home.
 */
fun NavGraphBuilder.editPetScreen(
    onBack: () -> Unit,
    onPetUpdated: () -> Unit,
    onPetDeleted: () -> Unit,
) {
    composable<EditPetDestination> {
        EditPetRoute(onBackClick = onBack, onPetUpdated = onPetUpdated, onPetDeleted = onPetDeleted)
    }
}
