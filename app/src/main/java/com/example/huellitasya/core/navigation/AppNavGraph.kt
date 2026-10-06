package com.example.huellitasya.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.huellitasya.features.auth.WelcomeScreen
import com.example.huellitasya.features.calendar.CalendarScreen
import com.example.huellitasya.features.home.HomeScreen
import com.example.huellitasya.features.profile.ProfileScreen
import com.example.huellitasya.features.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    // Obtenemos el estado de la ruta actual para saber dónde estamos
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Condición para mostrar la barra de navegación (solo en las secciones principales)
    val showBottomBar = currentDestination?.hasRoute<Destinations.Home>() == true ||
            currentDestination?.hasRoute<Destinations.Calendar>() == true ||
            currentDestination?.hasRoute<Destinations.Profile>() == true ||
            currentDestination?.hasRoute<Destinations.Menu>() == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HuellitasBottomBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Welcome,
            modifier = Modifier.padding(innerPadding) // Evita que el contenido quede debajo de la barra
        ) {
            // -- Flujo Auth --
            composable<Destinations.Welcome> {
                WelcomeScreen(onStartClick = { navController.navigate(Destinations.Home) })
            }
            composable<Destinations.Login> {
            }
            composable<Destinations.Register> {
            }

            // -- Flujo Principal (Con Barra Inferior) --
            composable<Destinations.Home> {
                HomeScreen()
            }
            composable<Destinations.Calendar> {
                CalendarScreen()
            }
            composable<Destinations.Profile> {
                ProfileScreen()
            }
            composable<Destinations.Menu> {
                SettingsScreen(
                    onLogout = { 
                        // Regresa al WelcomeScreen y limpia todo el historial de navegación
                        navController.navigate(Destinations.Welcome) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
