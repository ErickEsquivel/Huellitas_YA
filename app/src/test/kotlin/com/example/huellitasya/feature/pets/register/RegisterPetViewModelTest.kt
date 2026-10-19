package com.example.huellitasya.feature.pets.register

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.RegisterPetUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetBirthDateUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetBreedUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetWeightUseCase
import com.example.huellitasya.core.model.PetDraft
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakePetPhotoRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toFieldFeedback
import com.example.huellitasya.core.ui.toUiText
import com.example.huellitasya.feature.pets.form.MissingPetFieldsFeedback
import com.example.huellitasya.feature.pets.form.PetFormAction
import com.example.huellitasya.feature.pets.form.PetFormValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RegisterPetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository()
    private val photoRepository = FakePetPhotoRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope) = RegisterPetViewModel(
        registerPet = RegisterPetUseCase(authRepository, petRepository, photoRepository, appScope),
        validator = PetFormValidator(
            ValidateNameUseCase(),
            ValidatePetBreedUseCase(),
            ValidatePetBirthDateUseCase(),
            ValidatePetWeightUseCase(),
        ),
        networkMonitor = networkMonitor,
    )

    private fun RegisterPetViewModel.fillForm(weight: String = "5,5") {
        listOf(
            PetFormAction.NameChanged(" Maximiliano Tercero "),
            PetFormAction.SpeciesChanged(PetSpecies.DOG),
            PetFormAction.BreedChanged("Golden Retriever"),
            PetFormAction.BirthDateChanged(BirthDate),
            PetFormAction.SexChanged(PetSex.MALE),
            PetFormAction.WeightChanged(weight),
        ).forEach { onAction(RegisterPetAction.Form(it)) }
    }

    @Test
    fun `el formulario esta completo solo con todos los campos`() = runTest {
        val viewModel = viewModel(appScope = this)
        assertFalse(viewModel.uiState.value.form.isComplete)

        viewModel.fillForm()

        assertTrue(viewModel.uiState.value.form.isComplete)
    }

    @Test
    fun `con campos vacios se resaltan y aparece el aviso del formulario`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(RegisterPetAction.Form(PetFormAction.NameChanged("Max")))

        viewModel.onAction(RegisterPetAction.Save)

        val form = viewModel.uiState.value.form
        assertNull(form.nameFeedback)
        assertEquals(FieldStatus.Warning, form.speciesFeedback?.status)
        assertEquals(FieldStatus.Warning, form.weightFeedback?.status)
        assertEquals(MissingPetFieldsFeedback, form.formFeedback)
        assertTrue(petRepository.created.isEmpty())
    }

    @Test
    fun `editar un campo limpia su aviso y el del formulario`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(RegisterPetAction.Save)

        viewModel.onAction(RegisterPetAction.Form(PetFormAction.SpeciesChanged(PetSpecies.CAT)))

        assertNull(viewModel.uiState.value.form.speciesFeedback)
        assertNull(viewModel.uiState.value.form.formFeedback)
    }

    @Test
    fun `un peso invalido muestra su aviso y no guarda`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.fillForm(weight = "0")

        viewModel.onAction(RegisterPetAction.Save)

        assertEquals(ValidationError.InvalidWeight.toFieldFeedback(), viewModel.uiState.value.form.weightFeedback)
        assertNull(viewModel.uiState.value.form.formFeedback)
        assertTrue(petRepository.created.isEmpty())
    }

    @Test
    fun `guardar crea la mascota con los datos limpios y avisa para volver a Home`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.fillForm()
        viewModel.onAction(RegisterPetAction.Form(PetFormAction.PhotoPicked("content://media/7")))

        viewModel.events.test {
            viewModel.onAction(RegisterPetAction.Save)
            testScheduler.advanceUntilIdle()

            assertEquals(RegisterPetEvent.PetSaved, awaitItem())
        }
        val created = petRepository.created.single()
        assertEquals(
            PetDraft("Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", BirthDate, PetSex.MALE, 5.5),
            created.draft,
        )
        assertEquals("content://media/7", photoRepository.uploads.single().third)
        // El overlay sigue visible hasta que la pantalla se cierra.
        assertTrue(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `sin conexion avisa y no intenta guardar`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(appScope = this)
        viewModel.fillForm()

        viewModel.events.test {
            viewModel.onAction(RegisterPetAction.Save)
            testScheduler.advanceUntilIdle()

            assertEquals(RegisterPetEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertTrue(petRepository.created.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `si falla el guardado muestra el error y se puede reintentar`() = runTest {
        photoRepository.uploadResult = AppResult.Failure(DataError.FileTooLarge)
        val viewModel = viewModel(appScope = this)
        viewModel.fillForm()
        viewModel.onAction(RegisterPetAction.Form(PetFormAction.PhotoPicked("content://media/7")))

        viewModel.events.test {
            viewModel.onAction(RegisterPetAction.Save)
            testScheduler.advanceUntilIdle()

            assertEquals(RegisterPetEvent.ShowMessage(DataError.FileTooLarge.toUiText()), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private companion object {
        val BirthDate: LocalDate = LocalDate.of(2024, 1, 10)
    }
}
