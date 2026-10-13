package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.onSuccess
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.ProfilePhotoRepository
import com.example.huellitasya.core.domain.repository.UserRepository
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.model.User
import com.example.huellitasya.core.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Perfil del usuario con sesión (spec 004).
 *
 * Si `users/{uid}` aún no existe (el login lo está creando, o falló y se reintentará en el
 * siguiente), se muestran los datos de la sesión en lugar de un error.
 */
class ObserveUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AppResult<UserProfile, DataError>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user ->
                if (user == null) {
                    emptyFlow()
                } else {
                    userRepository.observeProfile(user.uid).map { result ->
                        if (result is AppResult.Failure && result.error == DataError.NotFound) {
                            AppResult.Success(user.toFallbackProfile())
                        } else {
                            result
                        }
                    }
                }
            }

    private fun User.toFallbackProfile() = UserProfile(
        uid = uid,
        name = name,
        email = email,
        phone = null,
        address = null,
        birthDate = null,
        photoUrl = photoUrl,
        provider = provider,
    )
}

/** Guarda los cambios ya validados de Datos Personales. Si cambió el nombre, también lo actualiza en Auth. */
class UpdateUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(uid: String, changes: ProfileChanges): AppResult<Unit, DataError> {
        if (changes.isEmpty) return AppResult.Success(Unit)
        return userRepository.updateProfile(uid, changes).onSuccess {
            // Best-effort: el perfil de Firestore es la fuente de verdad de las pantallas.
            changes.name?.let { authRepository.updateDisplayName(it) }
        }
    }
}

/**
 * Sube la foto elegida y guarda su URL en el perfil. Corre en [ApplicationScope] para que la
 * subida termine aunque el usuario cambie de pestaña mientras tanto.
 */
class ChangeProfilePhotoUseCase @Inject constructor(
    private val photoRepository: ProfilePhotoRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(uid: String, localUri: String): AppResult<Unit, DataError> =
        appScope.async {
            when (val upload = photoRepository.upload(uid, localUri)) {
                is AppResult.Success -> userRepository.updatePhotoUrl(uid, upload.data)
                is AppResult.Failure -> upload
            }
        }.await()
}
