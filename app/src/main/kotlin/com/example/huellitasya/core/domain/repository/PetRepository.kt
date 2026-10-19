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

    /** Id para una mascota nueva. Se genera antes de subir la foto, que usa la misma ruta. */
    fun newPetId(uid: String): String

    /** Crea `users/{uid}/pets/{petId}` con los datos del formulario. */
    suspend fun createPet(uid: String, petId: String, draft: PetDraft, photoUrl: String?): AppResult<Unit, DataError>
}
