package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.example.huellitasya.core.domain.repository.PetRepository
import com.example.huellitasya.core.domain.repository.RecordFileRepository
import com.example.huellitasya.core.domain.repository.RecordRepository
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

/** Una mascota del usuario con sesión, en vivo (perfil de la mascota, spec 005). */
class ObservePetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(petId: String): Flow<AppResult<Pet, DataError>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user -> if (user == null) emptyFlow() else petRepository.observePet(user.uid, petId) }
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

/**
 * Guarda los cambios de Editar mascota. Si eligió una foto nueva, la sube primero (reemplaza la
 * anterior en la misma ruta); si falla, no se cambia nada. Corre en [ApplicationScope] por la
 * misma razón que [RegisterPetUseCase].
 */
class UpdatePetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val photoRepository: PetPhotoRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(petId: String, draft: PetDraft, newPhotoUri: String?): AppResult<Unit, DataError> =
        appScope.async {
            val uid = authRepository.currentUser.first()?.uid
                ?: return@async AppResult.Failure(DataError.PermissionDenied)
            val photoUrl = if (newPhotoUri == null) {
                null
            } else {
                when (val upload = photoRepository.upload(uid, petId, newPhotoUri)) {
                    is AppResult.Success -> upload.data
                    is AppResult.Failure -> return@async upload
                }
            }
            petRepository.updatePet(uid, petId, draft, photoUrl)
        }.await()
}

/**
 * Elimina una mascota: primero sus registros médicos (archivo y documento, spec 006), luego su
 * foto (si no tenía, sigue) y al final el documento, para no dejar nada huérfano: el cliente no
 * borra subcolecciones. Corre en [ApplicationScope]: Editar vuelve a Home al terminar.
 */
class DeletePetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val photoRepository: PetPhotoRepository,
    private val recordRepository: RecordRepository,
    private val recordFileRepository: RecordFileRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(petId: String): AppResult<Unit, DataError> =
        appScope.async {
            val uid = authRepository.currentUser.first()?.uid
                ?: return@async AppResult.Failure(DataError.PermissionDenied)
            val records = when (val result = recordRepository.observeRecords(uid, petId).first()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@async result
            }
            for (record in records) {
                recordFileRepository.delete(uid, petId, record.id).let { if (it is AppResult.Failure) return@async it }
                recordRepository.deleteRecord(uid, petId, record.id).let { if (it is AppResult.Failure) return@async it }
            }
            when (val photo = photoRepository.delete(uid, petId)) {
                is AppResult.Success -> petRepository.deletePet(uid, petId)
                is AppResult.Failure -> photo
            }
        }.await()
}
