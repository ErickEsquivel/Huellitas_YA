package com.example.huellitasya.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.profile.ProfileRoute
import com.example.huellitasya.feature.profile.personaldata.PersonalDataRoute
import kotlinx.serialization.Serializable

@Serializable
data object ProfileDestination

/** Datos Personales (spec 004). Se abre desde Perfil y desde Configuración. */
@Serializable
data object PersonalDataDestination

fun NavGraphBuilder.profileScreen(onNavigateToPersonalData: () -> Unit) {
    composable<ProfileDestination> {
        ProfileRoute(onEditClick = onNavigateToPersonalData)
    }
}

fun NavGraphBuilder.personalDataScreen(onBack: () -> Unit) {
    composable<PersonalDataDestination> {
        PersonalDataRoute(onBackClick = onBack)
    }
}
