package com.example.huellitasya.feature.profile.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.profile.ProfileRoute
import com.example.huellitasya.feature.profile.personaldata.PersonalDataRoute
import com.example.huellitasya.feature.profile.verifyemail.VerifyEmailChangeRoute
import kotlinx.serialization.Serializable

@Serializable
data object ProfileDestination

/** Datos Personales (spec 004). Se abre desde Perfil y desde Configuración. */
@Serializable
data object PersonalDataDestination

/** "Verifica tu correo" del cambio de correo. `newEmail` es el correo al que se envió el código. */
@Serializable
data class VerifyEmailChangeDestination(val newEmail: String)

/** Clave con la que "Verifica tu correo" le devuelve el correo nuevo a Datos Personales. */
private const val EMAIL_CHANGED_RESULT = "email_changed"

fun NavGraphBuilder.profileScreen(onNavigateToPersonalData: () -> Unit) {
    composable<ProfileDestination> {
        ProfileRoute(onEditClick = onNavigateToPersonalData)
    }
}

fun NavGraphBuilder.personalDataScreen(
    onBack: () -> Unit,
    onNavigateToVerifyEmail: (newEmail: String) -> Unit,
) {
    composable<PersonalDataDestination> { entry ->
        val changedEmail by entry.savedStateHandle
            .getStateFlow<String?>(EMAIL_CHANGED_RESULT, null)
            .collectAsStateWithLifecycle()
        PersonalDataRoute(
            onBackClick = onBack,
            onNavigateToVerifyEmail = onNavigateToVerifyEmail,
            changedEmail = changedEmail,
            onChangedEmailShown = { entry.savedStateHandle[EMAIL_CHANGED_RESULT] = null },
        )
    }
}

fun NavGraphBuilder.verifyEmailChangeScreen(
    onBack: () -> Unit,
    onEmailChanged: (email: String) -> Unit,
) {
    composable<VerifyEmailChangeDestination> {
        VerifyEmailChangeRoute(onBackClick = onBack, onEmailChanged = onEmailChanged)
    }
}

/** Vuelve a Datos Personales avisándole del correo nuevo (muestra "Tu correo ahora es …"). */
fun NavController.popBackToPersonalData(changedEmail: String) {
    previousBackStackEntry?.savedStateHandle?.set(EMAIL_CHANGED_RESULT, changedEmail)
    popBackStack()
}
