package com.example.huellitasya.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.huellitasya.core.model.SessionState
import com.example.huellitasya.feature.auth.navigation.AuthGraph
import com.example.huellitasya.feature.auth.navigation.authGraph
import com.example.huellitasya.feature.calendar.navigation.calendarScreen
import com.example.huellitasya.feature.home.navigation.HomeDestination
import com.example.huellitasya.feature.home.navigation.homeScreen
import com.example.huellitasya.feature.profile.navigation.profileScreen
import com.example.huellitasya.feature.settings.navigation.settingsScreen

/**
 * Raíz de la UI. Cada estado de sesión tiene su propio NavHost: al iniciar o cerrar sesión
 * se reemplaza el grafo completo, así el historial anterior desaparece (atrás desde Home
 * no vuelve a Login y "Cerrar sesión" vuelve a Presentación con el historial limpio).
 */
@Composable
fun HuellitasApp(
    sessionState: SessionState,
    googleServerClientId: String?,
) {
    when (sessionState) {
        // El splash del sistema cubre este estado (ver MainActivity).
        SessionState.Loading -> Unit
        SessionState.SignedOut -> AuthNavHost(googleServerClientId)
        is SessionState.SignedIn -> MainNavHost()
    }
}

@Composable
private fun AuthNavHost(googleServerClientId: String?) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthGraph) {
        authGraph(navController = navController, googleServerClientId = googleServerClientId)
    }
}

@Composable
private fun MainNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // La barra inferior solo aparece en las pestañas principales.
    val showBottomBar = TopLevelDestination.entries.any { currentDestination?.hasRoute(it.route::class) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HuellitasBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(innerPadding), // Evita que el contenido quede debajo de la barra
        ) {
            homeScreen()
            calendarScreen()
            profileScreen()
            settingsScreen()
        }
    }
}
