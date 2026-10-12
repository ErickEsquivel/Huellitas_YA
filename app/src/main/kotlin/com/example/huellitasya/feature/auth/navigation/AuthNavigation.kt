package com.example.huellitasya.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.huellitasya.feature.auth.chooser.AuthChooserRoute
import com.example.huellitasya.feature.auth.connectivity.ConnectivityAware
import com.example.huellitasya.feature.auth.connectivity.ConnectivityViewModel
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
 * Todas las pantallas comparten el overlay "Sin conexión" ([ConnectivityAware]).
 *
 * @param googleServerClientId "ID de cliente web" de Firebase (`R.string.default_web_client_id`),
 *   o `null` si todavía no está configurado.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    googleServerClientId: String?,
) {
    navigation<AuthGraph>(startDestination = WelcomeDestination) {
        composable<WelcomeDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                WelcomeScreen(onStartClick = { navController.navigate(AuthChooserDestination) })
            }
        }
        composable<AuthChooserDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                AuthChooserRoute(
                    googleServerClientId = googleServerClientId,
                    onEmailClick = { navController.navigate(LoginDestination) },
                    onRegisterClick = { navController.navigate(RegisterDestination) },
                    onNetworkError = connectivity::onNetworkError,
                )
            }
        }
        composable<LoginDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                LoginRoute(
                    onBackClick = navController::navigateUp,
                    onForgotPasswordClick = { navController.navigate(ForgotPasswordDestination) },
                    onNetworkError = connectivity::onNetworkError,
                )
            }
        }
        composable<RegisterDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                RegisterRoute(
                    onBackClick = navController::navigateUp,
                    onNetworkError = connectivity::onNetworkError,
                )
            }
        }
        composable<ForgotPasswordDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                ForgotPasswordRoute(
                    onBackClick = navController::navigateUp,
                    onNetworkError = connectivity::onNetworkError,
                )
            }
        }
    }
}

/** El [ConnectivityViewModel] vive en la entrada de [AuthGraph]: un único estado para todo el flujo. */
@Composable
private fun authConnectivityViewModel(
    navController: NavController,
    entry: NavBackStackEntry,
): ConnectivityViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(AuthGraph) }
    return hiltViewModel(graphEntry)
}
