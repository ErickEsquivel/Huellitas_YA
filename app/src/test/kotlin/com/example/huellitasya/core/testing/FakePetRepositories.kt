package com.example.huellitasya.core.testing

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.example.huellitasya.core.domain.repository.PetRepository
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetDraft
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

val TestPetDraft = PetDraft(
    name = "Maximiliano Tercero",
    species = PetSpecies.DOG,
    breed = "Golden Retriever",
    birthDate = LocalDate.of(2026, 6, 6),
    sex = PetSex.MALE,
    weightKg = 5.0,
)

val TestPet = Pet(
    id = "pet-1",
    name = TestPetDraft.name,
    species = TestPetDraft.species,
    breed = TestPetDraft.breed,
    birthDate = TestPetDraft.birthDate,
    sex = TestPetDraft.sex,
    weightKg = TestPetDraft.weightKg,
    photoUrl = null,
)

/**
 * [PetRepository] en memoria. [pets] hace de colección `users/{uid}/pets`: crear una mascota
 * la agrega y la emite, igual que el snapshot listener de Firestore.
 */
class FakePetRepository : PetRepository {
    val pets = MutableStateFlow<AppResult<List<Pet>, DataError>>(AppResult.Success(emptyList()))
    var writeResult: AppResult<Unit, DataError> = AppResult.Success(Unit)

    /** (uid, petId, draft, photoUrl) de cada `createPet`. */
    val created = mutableListOf<CreatedPet>()
    private var nextId = 1

    override fun observePets(uid: String): Flow<AppResult<List<Pet>, DataError>> = pets

    override fun newPetId(uid: String): String = "pet-${nextId++}"

    override suspend fun createPet(
        uid: String,
        petId: String,
        draft: PetDraft,
        photoUrl: String?,
    ): AppResult<Unit, DataError> {
        created += CreatedPet(uid, petId, draft, photoUrl)
        val current = (pets.value as? AppResult.Success)?.data
        if (writeResult is AppResult.Success && current != null) {
            pets.value = AppResult.Success(current + draft.toPet(petId, photoUrl))
        }
        return writeResult
    }

    data class CreatedPet(val uid: String, val petId: String, val draft: PetDraft, val photoUrl: String?)
}

fun PetDraft.toPet(id: String, photoUrl: String? = null) = Pet(
    id = id,
    name = name,
    species = species,
    breed = breed,
    birthDate = birthDate,
    sex = sex,
    weightKg = weightKg,
    photoUrl = photoUrl,
)

class FakePetPhotoRepository : PetPhotoRepository {
    var uploadResult: AppResult<String, DataError> = AppResult.Success("https://storage.test/pet.jpg")

    /** (uid, petId, localUri) de cada subida. */
    val uploads = mutableListOf<Triple<String, String, String>>()

    override suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError> {
        uploads += Triple(uid, petId, localUri)
        return uploadResult
    }
}
