package com.example.huellitasya.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.huellitasya.MainUiState
import com.example.huellitasya.feature.auth.navigation.AuthGraph
import com.example.huellitasya.feature.auth.navigation.authGraph
import com.example.huellitasya.feature.calendar.navigation.calendarScreen
import com.example.huellitasya.feature.home.navigation.HomeDestination
import com.example.huellitasya.feature.home.navigation.homeScreen
import com.example.huellitasya.feature.pets.navigation.EditPetDestination
import com.example.huellitasya.feature.pets.navigation.PetDetailDestination
import com.example.huellitasya.feature.pets.navigation.editPetScreen
import com.example.huellitasya.feature.pets.navigation.PetsIntroDestination
import com.example.huellitasya.feature.pets.navigation.petDetailScreen
import com.example.huellitasya.feature.pets.navigation.petsIntroScreen
import com.example.huellitasya.feature.pets.navigation.RegisterPetDestination
import com.example.huellitasya.feature.pets.navigation.registerPetScreen
import com.example.huellitasya.feature.profile.navigation.PersonalDataDestination
import com.example.huellitasya.feature.profile.navigation.VerifyEmailChangeDestination
import com.example.huellitasya.feature.profile.navigation.personalDataScreen
import com.example.huellitasya.feature.profile.navigation.popBackToPersonalData
import com.example.huellitasya.feature.profile.navigation.profileScreen
import com.example.huellitasya.feature.profile.navigation.verifyEmailChangeScreen
import com.example.huellitasya.feature.settings.navigation.SecurityDestination
import com.example.huellitasya.feature.settings.navigation.securityScreen
import com.example.huellitasya.feature.settings.navigation.settingsScreen

/**
 * Raíz de la UI. Cada estado de sesión tiene su propio NavHost: al iniciar o cerrar sesión
 * se reemplaza el grafo completo, así el historial anterior desaparece (atrás desde Home
 * no vuelve a Login y "Cerrar sesión" vuelve a Bienvenido con el historial limpio).
 */
@Composable
fun HuellitasApp(
    uiState: MainUiState,
    googleServerClientId: String?,
) {
    when (uiState) {
        // Al abrir la app, el splash del sistema cubre este estado (ver MainActivity).
        MainUiState.Loading -> Unit
        MainUiState.SignedOut -> AuthNavHost(googleServerClientId)
        is MainUiState.SignedIn -> MainNavHost(googleServerClientId, startWithIntro = uiState.startWithIntro)
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
private fun MainNavHost(googleServerClientId: String?, startWithIntro: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // La barra inferior solo aparece en las pestañas principales.
    val showBottomBar = TopLevelDestination.entries.any { currentDestination?.hasRoute(it.route::class) == true }

    Scaffold(
        // Solo la barra de estado: sin barra inferior, cada pantalla se ocupa de la de navegación
        // (`navigationBarsPadding`) y su fondo llega hasta abajo, edge-to-edge.
        contentWindowInsets = WindowInsets.statusBars,
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
            // Sin mascotas, Presentación → Registrar mascota → Home (spec 005).
            startDestination = if (startWithIntro) PetsIntroDestination else HomeDestination,
            modifier = Modifier.padding(innerPadding), // Evita que el contenido quede debajo de la barra
        ) {
            homeScreen(
                onAddPetClick = { navController.navigate(RegisterPetDestination()) },
                onPetClick = { navController.navigate(PetDetailDestination(it)) },
            )
            petDetailScreen(
                onBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(EditPetDestination(it)) },
                onPetGone = { navController.popBackStack<HomeDestination>(inclusive = false) },
            )
            editPetScreen(
                onBack = { navController.popBackStack() },
                onPetUpdated = { navController.popBackStack() },
                onPetDeleted = { navController.popBackStack<HomeDestination>(inclusive = false) },
            )
            petsIntroScreen(onStartClick = { navController.navigate(RegisterPetDestination(fromIntro = true)) })
            registerPetScreen(
                onBack = { navController.popBackStack() },
                onPetSaved = { fromIntro ->
                    if (fromIntro) {
                        // Home reemplaza a Presentación y Registrar: "atrás" desde Home sale de la app.
                        navController.navigate(HomeDestination) {
                            popUpTo<PetsIntroDestination> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
            calendarScreen()
            profileScreen(onNavigateToPersonalData = { navController.navigate(PersonalDataDestination) })
            settingsScreen(
                googleServerClientId = googleServerClientId,
                onNavigateToPersonalData = { navController.navigate(PersonalDataDestination) },
                onNavigateToSecurity = { navController.navigate(SecurityDestination) },
            )
            securityScreen(onBack = { navController.popBackStack() })
            personalDataScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVerifyEmail = { navController.navigate(VerifyEmailChangeDestination(it)) },
            )
            verifyEmailChangeScreen(
                onBack = { navController.popBackStack() },
                onEmailChanged = { navController.popBackToPersonalData(it) },
            )
        }
    }
}
