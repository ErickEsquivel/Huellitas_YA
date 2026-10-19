package com.example.huellitasya.feature.pets.form

import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.error.isMissingValue
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetBirthDateUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetBreedUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetWeightUseCase
import com.example.huellitasya.core.model.PetDraft
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toFieldFeedback
import java.time.LocalDate
import javax.inject.Inject

/** Campos del formulario de mascota (Registrar y Editar, spec 005) con el aviso de cada uno. */
data class PetFormState(
    val name: String = "",
    val species: PetSpecies? = null,
    val breed: String = "",
    val birthDate: LocalDate? = null,
    val sex: PetSex? = null,
    val weight: String = "",
    /** Foto recién elegida en el selector (`content://`), aún sin subir. */
    val newPhotoUri: String? = null,
    /** Foto ya guardada en Storage (al editar). */
    val savedPhotoUrl: String? = null,
    val nameFeedback: FieldFeedback? = null,
    val speciesFeedback: FieldFeedback? = null,
    val breedFeedback: FieldFeedback? = null,
    val birthDateFeedback: FieldFeedback? = null,
    val sexFeedback: FieldFeedback? = null,
    val weightFeedback: FieldFeedback? = null,
    /** "Rellene todas las casillas indicadas" si falta algún campo. */
    val formFeedback: FieldFeedback? = null,
) {
    /** Figma pinta el botón en verde solo con todos los campos llenos (la foto es opcional). */
    val isComplete: Boolean
        get() = name.isNotBlank() && species != null && breed.isNotBlank() &&
            birthDate != null && sex != null && weight.isNotBlank()

    /** Lo que muestra el círculo de la foto: la recién elegida o la guardada. */
    val photoModel: String? get() = newPhotoUri ?: savedPhotoUrl
}

/** Cambios de un campo del formulario. Cada uno limpia su aviso y el del formulario. */
sealed interface PetFormAction {
    data class NameChanged(val value: String) : PetFormAction
    data class SpeciesChanged(val value: PetSpecies) : PetFormAction
    data class BreedChanged(val value: String) : PetFormAction
    data class BirthDateChanged(val value: LocalDate) : PetFormAction
    data class SexChanged(val value: PetSex) : PetFormAction
    data class WeightChanged(val value: String) : PetFormAction
    data class PhotoPicked(val uri: String) : PetFormAction
}

fun PetFormState.reduce(action: PetFormAction): PetFormState = when (action) {
    is PetFormAction.NameChanged -> copy(name = action.value, nameFeedback = null, formFeedback = null)
    is PetFormAction.SpeciesChanged -> copy(species = action.value, speciesFeedback = null, formFeedback = null)
    is PetFormAction.BreedChanged -> copy(breed = action.value, breedFeedback = null, formFeedback = null)
    is PetFormAction.BirthDateChanged -> copy(birthDate = action.value, birthDateFeedback = null, formFeedback = null)
    is PetFormAction.SexChanged -> copy(sex = action.value, sexFeedback = null, formFeedback = null)
    is PetFormAction.WeightChanged -> copy(weight = action.value, weightFeedback = null, formFeedback = null)
    is PetFormAction.PhotoPicked -> copy(newPhotoUri = action.uri)
}

/** Valida el formulario con las reglas de la spec 005. */
class PetFormValidator @Inject constructor(
    private val validateName: ValidateNameUseCase,
    private val validateBreed: ValidatePetBreedUseCase,
    private val validateBirthDate: ValidatePetBirthDateUseCase,
    private val validateWeight: ValidatePetWeightUseCase,
) {
    sealed interface Result {
        data class Valid(val draft: PetDraft) : Result

        /** El formulario con el aviso de cada campo inválido. */
        data class Invalid(val form: PetFormState) : Result
    }

    fun validate(form: PetFormState, today: LocalDate = LocalDate.now()): Result {
        val nameError = validateName(form.name)
        val speciesError = if (form.species == null) ValidationError.EmptySpecies else null
        val breedError = validateBreed(form.breed)
        val birthDateError = validateBirthDate(form.birthDate, today)
        val sexError = if (form.sex == null) ValidationError.EmptySex else null
        val weightError = validateWeight(form.weight)
        val errors = listOf(nameError, speciesError, breedError, birthDateError, sexError, weightError)

        if (errors.all { it == null }) {
            return Result.Valid(
                PetDraft(
                    name = form.name.trim(),
                    species = checkNotNull(form.species),
                    breed = form.breed.trim(),
                    birthDate = checkNotNull(form.birthDate),
                    sex = checkNotNull(form.sex),
                    weightKg = checkNotNull(validateWeight.parse(form.weight)),
                ),
            )
        }
        return Result.Invalid(
            form.copy(
                nameFeedback = nameError?.toFieldFeedback(),
                speciesFeedback = speciesError?.toFieldFeedback(),
                breedFeedback = breedError?.toFieldFeedback(),
                birthDateFeedback = birthDateError?.toFieldFeedback(),
                sexFeedback = sexError?.toFieldFeedback(),
                weightFeedback = weightError?.toFieldFeedback(),
                formFeedback = if (errors.any { it?.isMissingValue == true }) MissingPetFieldsFeedback else null,
            ),
        )
    }
}

/** Figma 158:1230: un solo aviso para todos los campos vacíos. */
val MissingPetFieldsFeedback = FieldFeedback(FieldStatus.Warning, UiText.Resource(R.string.error_missing_pet_fields))
