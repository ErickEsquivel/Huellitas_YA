package com.example.huellitasya.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.huellitasya.MainUiState
import com.example.huellitasya.feature.auth.navigation.AuthGraph
import com.example.huellitasya.feature.auth.navigation.authGraph
import com.example.huellitasya.feature.calendar.navigation.CalendarDestination
import com.example.huellitasya.feature.calendar.navigation.NewReminderDestination
import com.example.huellitasya.feature.calendar.navigation.NotificationsDestination
import com.example.huellitasya.feature.calendar.navigation.ReminderDetailDestination
import com.example.huellitasya.feature.calendar.navigation.calendarScreen
import com.example.huellitasya.feature.calendar.navigation.newReminderScreen
import com.example.huellitasya.feature.calendar.navigation.notificationsScreen
import com.example.huellitasya.feature.calendar.navigation.reminderDetailScreen
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
import com.example.huellitasya.feature.records.navigation.RecordDetailDestination
import com.example.huellitasya.feature.records.navigation.RecordListDestination
import com.example.huellitasya.feature.records.navigation.UploadRecordDestination
import com.example.huellitasya.feature.records.navigation.recordDetailScreen
import com.example.huellitasya.feature.records.navigation.recordListScreen
import com.example.huellitasya.feature.records.navigation.uploadRecordScreen
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
    openCalendarRequest: Int = 0,
) {
    when (uiState) {
        // Al abrir la app, el splash del sistema cubre este estado (ver MainActivity).
        MainUiState.Loading -> Unit
        MainUiState.SignedOut -> AuthNavHost(googleServerClientId)
        is MainUiState.SignedIn -> MainNavHost(googleServerClientId, startWithIntro = uiState.startWithIntro, openCalendarRequest = openCalendarRequest)
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
private fun MainNavHost(googleServerClientId: String?, startWithIntro: Boolean, openCalendarRequest: Int) {
    val navController = rememberNavController()

    // Tocar el aviso de un recordatorio abre el Calendario, como su pestaña (spec 007).
    LaunchedEffect(openCalendarRequest) {
        if (openCalendarRequest > 0) {
            navController.navigate(CalendarDestination) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
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
                onNotificationsClick = { navController.navigate(NotificationsDestination) },
            )
            petDetailScreen(
                onBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(EditPetDestination(it)) },
                onRecordsClick = { petId, category -> navController.navigate(RecordListDestination(petId, category)) },
                onPetGone = { navController.popBackStack<HomeDestination>(inclusive = false) },
            )
            recordListScreen(
                onBack = { navController.popBackStack() },
                onAddClick = { petId, category -> navController.navigate(UploadRecordDestination(petId, category)) },
                onRecordClick = { petId, recordId -> navController.navigate(RecordDetailDestination(petId, recordId)) },
                onPetGone = { navController.popBackStack<HomeDestination>(inclusive = false) },
            )
            recordDetailScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onPetGone = { navController.popBackStack<HomeDestination>(inclusive = false) },
            )
            uploadRecordScreen(
                onBack = { navController.popBackStack() },
                onUploaded = { navController.popBackStack() },
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
            calendarScreen(
                onNewReminderClick = { navController.navigate(NewReminderDestination(it)) },
                onReminderClick = { navController.navigate(ReminderDetailDestination(it)) },
                onNotificationsClick = { navController.navigate(NotificationsDestination) },
            )
            notificationsScreen(
                onBack = { navController.popBackStack() },
                onReminderClick = { navController.navigate(ReminderDetailDestination(it)) },
            )
            reminderDetailScreen(
                onBack = { navController.popBackStack() },
                onClosed = { navController.popBackStack() },
            )
            newReminderScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
            profileScreen(
                onNavigateToPersonalData = { navController.navigate(PersonalDataDestination) },
                onNotificationsClick = { navController.navigate(NotificationsDestination) },
            )
            settingsScreen(
                googleServerClientId = googleServerClientId,
                onNavigateToPersonalData = { navController.navigate(PersonalDataDestination) },
                onNavigateToSecurity = { navController.navigate(SecurityDestination) },
                onNotificationsClick = { navController.navigate(NotificationsDestination) },
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
