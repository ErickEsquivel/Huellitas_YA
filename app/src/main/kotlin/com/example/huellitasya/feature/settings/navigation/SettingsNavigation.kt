package com.example.huellitasya.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

/**
 * @param googleServerClientId "ID de cliente web" de Firebase, para reautenticar con Google antes
 *   de eliminar la cuenta (spec 004). `null` si Google no está configurado (spec 001).
 */
fun NavGraphBuilder.settingsScreen(
    googleServerClientId: String?,
    onNavigateToPersonalData: () -> Unit,
) {
    composable<SettingsDestination> {
        SettingsRoute(
            googleServerClientId = googleServerClientId,
            onPersonalDataClick = onNavigateToPersonalData,
        )
    }
}
