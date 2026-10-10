package com.example.huellitasya.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.huellitasya.feature.auth.chooser.AuthChooserRoute
import com.example.huellitasya.feature.auth.forgotpassword.ForgotPasswordRoute
import com.example.huellitasya.feature.auth.login.LoginRoute
import com.example.huellitasya.feature.auth.register.RegisterRoute
import com.example.huellitasya.feature.auth.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

/** Grafo anidado de autenticación (spec 002). */
@Serializable
data object AuthGraph

@Serializable
internal data object WelcomeDestination

@Serializable
internal data object AuthChooserDestination

@Serializable
internal data object LoginDestination

@Serializable
internal data object RegisterDestination

@Serializable
internal data object ForgotPasswordDestination

/**
 * Presentación → Bienvenido → (correo → Login → Recuperar contraseña | Regístrate | Google).
 * Al autenticarse no se navega desde aquí: la app reemplaza este grafo al cambiar la sesión.
 *
 * @param googleServerClientId "ID de cliente web" de Firebase (`R.string.default_web_client_id` de :app).
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    googleServerClientId: String,
) {
    navigation<AuthGraph>(startDestination = WelcomeDestination) {
        composable<WelcomeDestination> {
            WelcomeScreen(onStartClick = { navController.navigate(AuthChooserDestination) })
        }
        composable<AuthChooserDestination> {
            AuthChooserRoute(
                googleServerClientId = googleServerClientId,
                onEmailClick = { navController.navigate(LoginDestination) },
                onRegisterClick = { navController.navigate(RegisterDestination) },
            )
        }
        composable<LoginDestination> {
            LoginRoute(
                onBackClick = navController::navigateUp,
                onForgotPasswordClick = { navController.navigate(ForgotPasswordDestination) },
            )
        }
        composable<RegisterDestination> {
            RegisterRoute(onBackClick = navController::navigateUp)
        }
        composable<ForgotPasswordDestination> {
            ForgotPasswordRoute(onBackClick = navController::navigateUp)
        }
    }
}
