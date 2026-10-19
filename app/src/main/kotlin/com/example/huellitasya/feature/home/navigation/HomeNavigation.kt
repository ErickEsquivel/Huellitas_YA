package com.example.huellitasya.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(
    onAddPetClick: () -> Unit,
    onPetClick: (petId: String) -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(onAddPetClick = onAddPetClick, onPetClick = onPetClick)
    }
}
