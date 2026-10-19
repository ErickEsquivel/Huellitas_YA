package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.example.huellitasya.core.domain.repository.PetRepository
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Mascotas del usuario con sesión, en vivo (spec 005). Sin sesión no emite nada. */
class ObservePetsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AppResult<List<Pet>, DataError>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user -> if (user == null) emptyFlow() else petRepository.observePets(user.uid) }
}

/**
 * Presentación tras el login (spec 005): se muestra si la primera lectura de las mascotas viene
 * vacía. Si la lectura falla o tarda más de [TIMEOUT_MS], se entra a Home.
 */
class ShouldShowPetsIntroUseCase @Inject constructor(
    private val observePets: ObservePetsUseCase,
) {
    suspend operator fun invoke(): Boolean {
        val first = withTimeoutOrNull(TIMEOUT_MS) { observePets().first() }
        return first is AppResult.Success && first.data.isEmpty()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

/**
 * Registra una mascota: genera su id, sube la foto (si eligió una) y crea el documento.
 * Si la foto falla no se crea nada, para que el usuario reintente con todo el formulario.
 * Corre en [ApplicationScope]: Registrar navega a Home al terminar, y si el usuario sale
 * antes, el guardado termina igual.
 */
class RegisterPetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val photoRepository: PetPhotoRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(draft: PetDraft, photoUri: String?): AppResult<Unit, DataError> =
        appScope.async {
            val uid = authRepository.currentUser.first()?.uid
                ?: return@async AppResult.Failure(DataError.PermissionDenied)
            val petId = petRepository.newPetId(uid)
            val photoUrl = if (photoUri == null) {
                null
            } else {
                when (val upload = photoRepository.upload(uid, petId, photoUri)) {
                    is AppResult.Success -> upload.data
                    is AppResult.Failure -> return@async upload
                }
            }
            petRepository.createPet(uid, petId, draft, photoUrl)
        }.await()
}
