package com.example.huellitasya.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.settings.SettingsRoute
import com.example.huellitasya.feature.settings.security.SecurityRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

/** Configuración → Seguridad: cambio de contraseña (spec 004). */
@Serializable
data object SecurityDestination

/**
 * @param googleServerClientId "ID de cliente web" de Firebase, para reautenticar con Google antes
 *   de eliminar la cuenta (spec 004). `null` si Google no está configurado (spec 001).
 */
fun NavGraphBuilder.settingsScreen(
    googleServerClientId: String?,
    onNavigateToPersonalData: () -> Unit,
    onNavigateToSecurity: () -> Unit,
) {
    composable<SettingsDestination> {
        SettingsRoute(
            googleServerClientId = googleServerClientId,
            onPersonalDataClick = onNavigateToPersonalData,
            onSecurityClick = onNavigateToSecurity,
        )
    }
}

fun NavGraphBuilder.securityScreen(onBack: () -> Unit) {
    composable<SecurityDestination> {
        SecurityRoute(onBackClick = onBack)
    }
}
