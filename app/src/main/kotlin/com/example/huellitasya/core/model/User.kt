package com.example.huellitasya.core.model

/** Usuario autenticado. Su perfil se persiste en Firestore en `users/{uid}`. */
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val provider: AuthProvider,
)

enum class AuthProvider { EMAIL, GOOGLE }
