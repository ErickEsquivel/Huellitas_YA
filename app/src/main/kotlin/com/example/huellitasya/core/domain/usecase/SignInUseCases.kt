package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.onSuccess
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.UserRepository
import com.example.huellitasya.core.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject

/*
 * En cuanto Firebase autentica, la sesión cambia y la app reemplaza el grafo de auth por el
 * principal, lo que destruye el ViewModel que llamó al use case. Por eso el trabajo se lanza
 * en [ApplicationScope]: garantiza que el perfil `users/{uid}` se cree aunque la pantalla ya no exista.
 * Si crear el perfil falla, el inicio de sesión sigue siendo válido: el siguiente login lo reintenta.
 */

class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<User, AuthError> =
        appScope.async {
            authRepository.signInWithEmail(email.trim(), password)
                .onSuccess { userRepository.ensureProfile(it) }
        }.await()
}

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(name: String, email: String, password: String): AppResult<User, AuthError> =
        appScope.async {
            authRepository.registerWithEmail(name.trim(), email.trim(), password)
                .onSuccess { userRepository.ensureProfile(it) }
        }.await()
}

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(idToken: String): AppResult<User, AuthError> =
        appScope.async {
            authRepository.signInWithGoogle(idToken)
                .onSuccess { userRepository.ensureProfile(it) }
        }.await()
}

/** Crea `users/{uid}` si falta y copia el correo de Auth si cambió (spec 004, cambio de correo). */
private suspend fun UserRepository.ensureProfile(user: User) {
    createIfMissing(user)
    if (user.email.isNotBlank()) syncEmail(user.uid, user.email)
}
