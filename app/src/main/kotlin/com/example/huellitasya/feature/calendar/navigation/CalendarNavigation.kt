package com.example.huellitasya.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.calendar.CalendarRoute
import kotlinx.serialization.Serializable

@Serializable
data object CalendarDestination

/**
 * Calendario (spec 007).
 * @param onAddClick "+": abre "¿Qué deseas programar?…".
 * @param onReminderClick abre el detalle del recordatorio.
 */
fun NavGraphBuilder.calendarScreen(
    onAddClick: () -> Unit,
    onReminderClick: (reminderId: String) -> Unit,
    onNotificationsClick: () -> Unit,
) {
    composable<CalendarDestination> {
        CalendarRoute(
            onAddClick = onAddClick,
            onReminderClick = onReminderClick,
            onNotificationsClick = onNotificationsClick,
        )
    }
}
