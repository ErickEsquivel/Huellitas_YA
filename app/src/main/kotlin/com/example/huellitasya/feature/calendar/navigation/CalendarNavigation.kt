package com.example.huellitasya.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.feature.calendar.CalendarRoute
import com.example.huellitasya.feature.calendar.detail.ReminderDetailRoute
import com.example.huellitasya.feature.calendar.newreminder.NewReminderRoute
import kotlinx.serialization.Serializable

@Serializable
data object CalendarDestination

/**
 * Calendario (spec 007). El "+" abre "¿Qué deseas programar?…" dentro de la misma pantalla.
 * @param onNewReminderClick se eligió el tipo en la hoja: abre "Nuevo recordatorio".
 * @param onReminderClick abre el detalle del recordatorio.
 */
fun NavGraphBuilder.calendarScreen(
    onNewReminderClick: (ReminderKind) -> Unit,
    onReminderClick: (reminderId: String) -> Unit,
    onNotificationsClick: () -> Unit,
) {
    composable<CalendarDestination> {
        CalendarRoute(
            onNewReminderClick = onNewReminderClick,
            onReminderClick = onReminderClick,
            onNotificationsClick = onNotificationsClick,
        )
    }
}

/** "Nuevo recordatorio". La propiedad es la clave que lee `NewReminderViewModel`: el nombre de [ReminderKind]. */
@Serializable
data class NewReminderDestination(val kind: String) {
    constructor(kind: ReminderKind) : this(kind.name)
}

/** @param onSaved el recordatorio quedó guardado: vuelve al Calendario. */
fun NavGraphBuilder.newReminderScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    composable<NewReminderDestination> {
        NewReminderRoute(onBackClick = onBack, onSaved = onSaved)
    }
}

/** Detalle de un recordatorio. La propiedad es la clave que lee `ReminderDetailViewModel`. */
@Serializable
data class ReminderDetailDestination(val reminderId: String)

/** @param onClosed se actualizó, se eliminó o ya no existe: vuelve atrás. */
fun NavGraphBuilder.reminderDetailScreen(
    onBack: () -> Unit,
    onClosed: () -> Unit,
) {
    composable<ReminderDetailDestination> {
        ReminderDetailRoute(onBackClick = onBack, onClosed = onClosed)
    }
}
