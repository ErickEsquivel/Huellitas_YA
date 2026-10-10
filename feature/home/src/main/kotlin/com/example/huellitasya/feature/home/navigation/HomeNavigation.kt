package com.example.huellitasya.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen() {
    composable<HomeDestination> {
        HomeScreen()
    }
}
