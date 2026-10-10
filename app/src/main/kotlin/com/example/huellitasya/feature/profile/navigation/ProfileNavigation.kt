package com.example.huellitasya.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.profile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileDestination

fun NavGraphBuilder.profileScreen() {
    composable<ProfileDestination> {
        ProfileScreen()
    }
}
