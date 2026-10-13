package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toAuthError
import com.example.huellitasya.core.data.mapper.toUser
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.toUser()) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<User, AuthError> =
        runAuth {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            checkNotNull(result.user).toUser()
        }

    override suspend fun registerWithEmail(name: String, email: String, password: String): AppResult<User, AuthError> =
        runAuth {
            val firebaseUser = checkNotNull(firebaseAuth.createUserWithEmailAndPassword(email, password).await().user)
            firebaseUser.updateProfile(userProfileChangeRequest { displayName = name }).await()
            // `toUser()` tomaría el displayName antes de recargar; el nombre del formulario es la fuente de verdad.
            firebaseUser.toUser().copy(name = name)
        }

    override suspend fun signInWithGoogle(idToken: String): AppResult<User, AuthError> =
        runAuth {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            checkNotNull(firebaseAuth.signInWithCredential(credential).await().user).toUser()
        }

    override suspend fun reauthenticateWithPassword(password: String): AppResult<Unit, AuthError> =
        runAuth {
            val user = checkNotNull(firebaseAuth.currentUser) { "Sin sesión" }
            val credential = EmailAuthProvider.getCredential(checkNotNull(user.email), password)
            user.reauthenticate(credential).await()
            Unit
        }

    override suspend fun reauthenticateWithGoogle(idToken: String): AppResult<Unit, AuthError> =
        runAuth {
            val user = checkNotNull(firebaseAuth.currentUser) { "Sin sesión" }
            user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
            Unit
        }

    override suspend fun updatePassword(newPassword: String): AppResult<Unit, AuthError> =
        runAuth {
            val user = checkNotNull(firebaseAuth.currentUser) { "Sin sesión" }
            user.updatePassword(newPassword).await()
            Unit
        }

    override suspend fun reload(): AppResult<Unit, AuthError> =
        runAuth {
            val user = checkNotNull(firebaseAuth.currentUser) { "Sin sesión" }
            user.reload().await()
            // El token nuevo trae el correo actualizado (lo usan las reglas de Firestore).
            user.getIdToken(true).await()
            Unit
        }

    override suspend fun updateDisplayName(name: String): AppResult<Unit, AuthError> =
        runAuth {
            val user = checkNotNull(firebaseAuth.currentUser) { "Sin sesión" }
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()
            Unit
        }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private inline fun <T> runAuth(block: () -> T): AppResult<T, AuthError> =
        try {
            AppResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toAuthError())
        }
}
