package com.example.huellitasya.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.feature.calendar.CalendarScreen
import kotlinx.serialization.Serializable

@Serializable
data object CalendarDestination

fun NavGraphBuilder.calendarScreen() {
    composable<CalendarDestination> {
        CalendarScreen()
    }
}
