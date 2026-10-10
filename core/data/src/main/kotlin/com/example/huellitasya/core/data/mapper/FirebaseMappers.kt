package com.example.huellitasya.core.data.mapper

import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.User
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException

internal fun FirebaseUser.toUser(): User = User(
    uid = uid,
    name = displayName.orEmpty(),
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString(),
    provider = if (providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
        AuthProvider.GOOGLE
    } else {
        AuthProvider.EMAIL
    },
)

/** Tabla "Errores remotos" de la spec 002. El orden importa: WeakPassword hereda de InvalidCredentials. */
internal fun Throwable.toAuthError(): AuthError = when (this) {
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
    is FirebaseNetworkException -> AuthError.Network
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    else -> AuthError.Unknown
}

internal fun Throwable.toDataError(): DataError = when {
    this is FirebaseNetworkException -> DataError.Network
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE -> DataError.Network
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.PermissionDenied
    else -> DataError.Unknown
}
