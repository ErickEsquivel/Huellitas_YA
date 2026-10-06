package com.example.huellitasya.core.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenPrimary

// Modelo para los items de la barra
data class BottomNavItem(
    val title: String,
    val iconRes: Int,
    val route: Destinations
)

@Composable
fun HuellitasBottomBar(
    navController: NavController,
    currentDestination: NavDestination?
) {
    val items = listOf(
        BottomNavItem("Home", R.drawable.ic_nav_home, Destinations.Home),
        BottomNavItem("Calendar", R.drawable.ic_nav_calendar, Destinations.Calendar),
        BottomNavItem("Profile", R.drawable.ic_nav_profile, Destinations.Profile),
        BottomNavItem("Menu", R.drawable.ic_nav_menu, Destinations.Menu)
    )

    NavigationBar(
        containerColor = Color.White,
        // Material 3 tiene un borde (tonal elevation) por defecto, si quieres que sea plana pon tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            // Evitar acumular múltiples copias de la misma vista
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Evita que se abra varias veces seguidas si se hace click rápido
                            launchSingleTop = true
                            // Restaura el estado previo al volver a la pestaña
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(36.dp) // Aumentamos el tamaño de los íconos
                    )
                },
                // Desactivamos el texto para que solo se vean los iconos (como en tu Figma)
                //showLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenPrimary,     // Color cuando está en la vista actual
                    unselectedIconColor = GreenBackground, // Color para los demás íconos
                    // Ocultamos el "globo" por defecto de Material 3 detrás del ícono seleccionado
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
