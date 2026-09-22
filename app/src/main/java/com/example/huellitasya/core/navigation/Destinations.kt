package com.example.huellitasya.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destinations {
    @Serializable
    data object Welcome : Destinations()

    @Serializable
    data object Login : Destinations()

    @Serializable
    data object Register : Destinations()

    @Serializable
    data object Home : Destinations()

    @Serializable
    data object Profile : Destinations()

    @Serializable
    data class PetDetail(val petId: String) : Destinations()
}
