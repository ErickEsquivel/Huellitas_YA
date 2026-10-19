package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetDraft
import kotlinx.coroutines.flow.Flow

/** Mascotas del usuario en `users/{uid}/pets` (spec 005). */
interface PetRepository {
    /** Emite la lista, ordenada por fecha de registro, cada vez que cambia; sin red, desde la caché. */
    fun observePets(uid: String): Flow<AppResult<List<Pet>, DataError>>

    /** Emite la mascota cada vez que cambia; `NotFound` si no existe o se eliminó. */
    fun observePet(uid: String, petId: String): Flow<AppResult<Pet, DataError>>

    /** Id para una mascota nueva. Se genera antes de subir la foto, que usa la misma ruta. */
    fun newPetId(uid: String): String

    /** Crea `users/{uid}/pets/{petId}` con los datos del formulario. */
    suspend fun createPet(uid: String, petId: String, draft: PetDraft, photoUrl: String?): AppResult<Unit, DataError>

    /** Reemplaza los datos del formulario y, si [photoUrl] no es `null`, la foto. No toca `createdAt`. */
    suspend fun updatePet(uid: String, petId: String, draft: PetDraft, photoUrl: String?): AppResult<Unit, DataError>

    /** Borra `users/{uid}/pets/{petId}`. */
    suspend fun deletePet(uid: String, petId: String): AppResult<Unit, DataError>
}
