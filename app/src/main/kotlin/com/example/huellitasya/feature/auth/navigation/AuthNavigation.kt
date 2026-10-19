package com.example.huellitasya.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.huellitasya.feature.auth.newpassword.NewPasswordRoute
import com.example.huellitasya.feature.auth.register.RegisterRoute
import com.example.huellitasya.feature.auth.verifycode.VerifyResetCodeRoute
import kotlinx.serialization.Serializable

/** Grafo anidado de autenticación (spec 002). */
@Serializable
data object AuthGraph

@Serializable
internal data object AuthChooserDestination

@Serializable
internal data object LoginDestination

@Serializable
internal data object RegisterDestination

@Serializable
internal data object ForgotPasswordDestination

/** "Verifica tu correo": el nombre de la propiedad es la clave que lee `VerifyResetCodeViewModel`. */
@Serializable
internal data class VerifyResetCodeDestination(val email: String)

/** "Crear nueva contraseña": las propiedades son las claves que lee `NewPasswordViewModel`. */
@Serializable
internal data class NewPasswordDestination(val email: String, val resetToken: String)

/** Clave en el `savedStateHandle` de Login para mostrar "Tu contraseña se actualizó". */
private const val PASSWORD_RESET_KEY = "password_reset"

/**
 * Bienvenido → (correo → Login → Recuperar contraseña | Regístrate | Google). Presentación ya no
 * está aquí: se ve después del login si no hay mascotas (spec 005).
 * Recuperar contraseña → Verifica tu correo (código) → Crear nueva contraseña → vuelve a Login.
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
    navigation<AuthGraph>(startDestination = AuthChooserDestination) {
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
            val passwordReset by entry.savedStateHandle
                .getStateFlow(PASSWORD_RESET_KEY, false)
                .collectAsStateWithLifecycle()
            ConnectivityAware(connectivity) {
                LoginRoute(
                    onBackClick = navController::navigateUp,
                    onForgotPasswordClick = { navController.navigate(ForgotPasswordDestination) },
                    onNetworkError = connectivity::onNetworkError,
                    showPasswordResetMessage = passwordReset,
                    onPasswordResetMessageShown = { entry.savedStateHandle[PASSWORD_RESET_KEY] = false },
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
                    onCodeSent = { email -> navController.navigate(VerifyResetCodeDestination(email)) },
                    onNetworkError = connectivity::onNetworkError,
                )
            }
        }
        composable<VerifyResetCodeDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                VerifyResetCodeRoute(
                    onBackClick = navController::navigateUp,
                    onCodeVerified = { email, resetToken ->
                        // El código ya se usó: atrás desde "Crear nueva contraseña" vuelve a "¿Olvidaste…?".
                        navController.navigate(NewPasswordDestination(email, resetToken)) {
                            popUpTo<VerifyResetCodeDestination> { inclusive = true }
                        }
                    },
                    onNetworkError = connectivity::onNetworkError,
                )
            }
        }
        composable<NewPasswordDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry)
            ConnectivityAware(connectivity) {
                NewPasswordRoute(
                    onBackClick = navController::navigateUp,
                    onPasswordReset = {
                        navController.getBackStackEntry<LoginDestination>().savedStateHandle[PASSWORD_RESET_KEY] = true
                        navController.popBackStack<LoginDestination>(inclusive = false)
                    },
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
