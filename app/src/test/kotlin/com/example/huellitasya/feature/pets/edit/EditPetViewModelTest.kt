package com.example.huellitasya.feature.pets.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.DeletePetUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.UpdatePetUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetBirthDateUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetBreedUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetWeightUseCase
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakePetPhotoRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeRecordFileRepository
import com.example.huellitasya.core.testing.FakeRecordRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import com.example.huellitasya.feature.pets.form.MissingPetFieldsFeedback
import com.example.huellitasya.feature.pets.form.PetFormAction
import com.example.huellitasya.feature.pets.form.PetFormValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditPetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val pet = TestPet.copy(weightKg = 4.5, photoUrl = "https://storage.test/vieja.jpg")
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(pet)) }
    private val photoRepository = FakePetPhotoRepository()
    private val recordRepository = FakeRecordRepository()
    private val recordFileRepository = FakeRecordFileRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope) = EditPetViewModel(
        savedStateHandle = SavedStateHandle(mapOf(EditPetViewModel.PET_ID_ARG to pet.id)),
        observePet = ObservePetUseCase(authRepository, petRepository),
        updatePet = UpdatePetUseCase(authRepository, petRepository, photoRepository, appScope),
        deletePet = DeletePetUseCase(authRepository, petRepository, photoRepository, recordRepository, recordFileRepository, appScope),
        validator = PetFormValidator(
            ValidateNameUseCase(),
            ValidatePetBreedUseCase(),
            ValidatePetBirthDateUseCase(),
            ValidatePetWeightUseCase(),
        ),
        validateWeight = ValidatePetWeightUseCase(),
        networkMonitor = networkMonitor,
    )

    private fun EditPetViewModel.form(action: PetFormAction) = onAction(EditPetAction.Form(action))

    @Test
    fun `carga la mascota en el formulario sin cambios`() = runTest {
        val state = viewModel(appScope = this).uiState.value

        assertEquals(pet.name, state.form.name)
        assertEquals("4,5", state.form.weight)
        assertEquals(pet.photoUrl, state.form.photoModel)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `hay cambios solo si algo difiere de lo guardado`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.form(PetFormAction.WeightChanged("4.5"))
        assertFalse(viewModel.uiState.value.hasChanges)

        viewModel.form(PetFormAction.SexChanged(PetSex.FEMALE))
        assertTrue(viewModel.uiState.value.hasChanges)

        viewModel.form(PetFormAction.SexChanged(pet.sex))
        assertFalse(viewModel.uiState.value.hasChanges)

        viewModel.form(PetFormAction.PhotoPicked("content://nueva"))
        assertTrue(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `sin cambios Actualizar no hace nada`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(EditPetAction.Update)
        testScheduler.advanceUntilIdle()

        assertTrue(petRepository.updated.isEmpty())
    }

    @Test
    fun `un campo vaciado muestra el aviso y no guarda`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.form(PetFormAction.BreedChanged(""))

        viewModel.onAction(EditPetAction.Update)

        assertEquals(MissingPetFieldsFeedback, viewModel.uiState.value.form.formFeedback)
        assertTrue(petRepository.updated.isEmpty())
    }

    @Test
    fun `actualizar guarda los cambios y vuelve al perfil`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.form(PetFormAction.WeightChanged("6"))

        viewModel.events.test {
            viewModel.onAction(EditPetAction.Update)
            testScheduler.advanceUntilIdle()

            assertEquals(EditPetEvent.Updated, awaitItem())
        }
        val (petId, draft, photoUrl) = petRepository.updated.single()
        assertEquals(pet.id, petId)
        assertEquals(6.0, draft.weightKg, 0.0)
        assertEquals(null, photoUrl)
    }

    @Test
    fun `eliminar pide confirmacion, borra y vuelve a Home`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(EditPetAction.DeleteClick)
        assertTrue(viewModel.uiState.value.showDeleteDialog)

        viewModel.events.test {
            viewModel.onAction(EditPetAction.ConfirmDelete)
            testScheduler.advanceUntilIdle()

            assertEquals(EditPetEvent.Deleted, awaitItem())
        }
        assertEquals(listOf(pet.id), petRepository.deleted)
        assertEquals(listOf(pet.id), photoRepository.deletedPhotos)
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `cerrar el dialogo no elimina`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(EditPetAction.DeleteClick)

        viewModel.onAction(EditPetAction.DismissDelete)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertTrue(petRepository.deleted.isEmpty())
    }

    @Test
    fun `sin conexion avisa y no actualiza ni elimina`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(appScope = this)
        viewModel.form(PetFormAction.WeightChanged("6"))

        viewModel.events.test {
            viewModel.onAction(EditPetAction.Update)
            testScheduler.advanceUntilIdle()
            assertEquals(EditPetEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())

            viewModel.onAction(EditPetAction.DeleteClick)
            viewModel.onAction(EditPetAction.ConfirmDelete)
            testScheduler.advanceUntilIdle()
            assertEquals(EditPetEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertTrue(petRepository.updated.isEmpty())
        assertTrue(petRepository.deleted.isEmpty())
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `si la mascota ya no existe vuelve a Home`() = runTest {
        petRepository.pets.value = AppResult.Success(emptyList())

        viewModel(appScope = this).events.test {
            assertEquals(EditPetEvent.Deleted, awaitItem())
        }
    }
}
