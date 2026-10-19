package com.example.huellitasya.navigation

import androidx.annotation.DrawableRes
import com.example.huellitasya.R
import com.example.huellitasya.feature.calendar.navigation.CalendarDestination
import com.example.huellitasya.feature.home.navigation.HomeDestination
import com.example.huellitasya.feature.profile.navigation.ProfileDestination
import com.example.huellitasya.feature.settings.navigation.SettingsDestination

/**
 * Pestañas de la barra inferior. Agregar una pestaña = nueva entrada aquí + su `xScreen()` en el NavHost.
 *
 * @param isEnabled `false` = la pestaña se ve atenuada y no navega. Calendario está inhabilitado
 *   hasta que tenga su spec.
 */
enum class TopLevelDestination(
    val title: String,
    @param:DrawableRes val iconRes: Int,
    val route: Any,
    val isEnabled: Boolean = true,
) {
    HOME("Home", R.drawable.ic_nav_home, HomeDestination),
    CALENDAR("Calendar", R.drawable.ic_nav_calendar, CalendarDestination, isEnabled = false),
    PROFILE("Profile", R.drawable.ic_nav_profile, ProfileDestination),
    SETTINGS("Menu", R.drawable.ic_nav_menu, SettingsDestination),
}
