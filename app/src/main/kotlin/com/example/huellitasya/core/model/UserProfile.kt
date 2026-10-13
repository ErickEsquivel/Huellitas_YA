package com.example.huellitasya.core.model

import java.time.LocalDate

/** Perfil del dueño guardado en `users/{uid}` (spec 004). Los campos opcionales son `null` si no se han registrado. */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val birthDate: LocalDate?,
    val photoUrl: String?,
    val provider: AuthProvider,
)

/** Cambios a guardar en el perfil. Un campo en `null` no cambia. */
data class ProfileChanges(
    val name: String? = null,
    val phone: Field<String>? = null,
    val address: Field<String>? = null,
    val birthDate: Field<LocalDate>? = null,
) {
    val isEmpty: Boolean get() = name == null && phone == null && address == null && birthDate == null
}

/** Nuevo valor de un campo opcional: se asigna ([Set]) o se borra del documento ([Clear]). */
sealed interface Field<out T> {
    data class Set<out T>(val value: T) : Field<T>
    data object Clear : Field<Nothing>
}
