package com.example.huellitasya.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.huellitasya.features.auth.WelcomeScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.Welcome
    ) {
        composable<Destinations.Welcome> {
            WelcomeScreen(onStartClick = { navController.navigate(Destinations.Login) })
        }
        composable<Destinations.Login> {
            // LoginScreen(onNavigateToHome = { navController.navigate(Destinations.Home) })
        }
        composable<Destinations.Register> {
            // RegisterScreen()
        }
        composable<Destinations.Home> {
            // HomeScreen(onNavigateToPetDetail = { petId -> navController.navigate(Destinations.PetDetail(petId)) })
        }
        composable<Destinations.Profile> {
            // ProfileScreen()
        }
        composable<Destinations.PetDetail> {
            // val args = it.toRoute<Destinations.PetDetail>()
            // PetDetailScreen(petId = args.petId)
        }
    }
}