package com.example.huellitasya.feature.calendar.newreminder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.CreateReminderUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.validation.ValidateReminderUseCase
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.model.VetReason
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeReminderRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toFieldFeedback
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class NewReminderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val secondPet = TestPet.copy(id = "pet-2", name = "Maximiliano Cuarto")
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet, secondPet)) }
    private val reminderRepository = FakeReminderRepository()
    private val networkMonitor = FakeNetworkMonitor()

    /** "Ahora" es el 1 de noviembre de 2026 a mediodía. */
    private val clock = Clock.fixed(
        LocalDateTime.of(2026, 11, 1, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
    )
    private val tomorrow = LocalDate.of(2026, 11, 2)

    private fun viewModel(appScope: CoroutineScope, kind: ReminderKind) = NewReminderViewModel(
        savedStateHandle = SavedStateHandle(mapOf(NewReminderViewModel.KIND_ARG to kind.name)),
        observePets = ObservePetsUseCase(authRepository, petRepository),
        createReminder = CreateReminderUseCase(authRepository, reminderRepository, appScope),
        validateReminder = ValidateReminderUseCase(),
        networkMonitor = networkMonitor,
        clock = clock,
    )

    private fun NewReminderViewModel.fillDateTime(date: LocalDate = tomorrow, time: LocalTime = LocalTime.of(11, 0)) {
        onAction(NewReminderAction.DateChanged(date))
        onAction(NewReminderAction.TimeChanged(time))
    }

    @Test
    fun `con una sola mascota viene elegida y con varias ninguna`() = runTest {
        assertNull(viewModel(this, ReminderKind.VACCINE).uiState.value.selectedPetId)

        petRepository.pets.value = AppResult.Success(listOf(TestPet))
        assertEquals(TestPet.id, viewModel(this, ReminderKind.VACCINE).uiState.value.selectedPetId)
    }

    @Test
    fun `el boton se habilita solo con el formulario completo`() = runTest {
        val viewModel = viewModel(this, ReminderKind.VACCINE)
        assertFalse(viewModel.uiState.value.isComplete)

        viewModel.onAction(NewReminderAction.PetSelected(secondPet.id))
        viewModel.onAction(NewReminderAction.VaccineSelected(VaccineType.RABIES))
        viewModel.fillDateTime()

        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `elegir Otros pide el nombre`() = runTest {
        val viewModel = viewModel(this, ReminderKind.VET)
        viewModel.onAction(NewReminderAction.PetSelected(TestPet.id))
        viewModel.onAction(NewReminderAction.ToggleOptions)
        viewModel.onAction(NewReminderAction.VetReasonSelected(VetReason.OTHER))
        viewModel.fillDateTime()

        assertFalse(viewModel.uiState.value.isOptionsOpen)
        assertTrue(viewModel.uiState.value.needsTitle)
        assertFalse(viewModel.uiState.value.isComplete)

        viewModel.onAction(NewReminderAction.TitleChanged("Control de peso"))
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `guardar una cita crea el recordatorio y vuelve al calendario`() = runTest {
        val viewModel = viewModel(this, ReminderKind.VET)
        viewModel.onAction(NewReminderAction.PetSelected(secondPet.id))
        viewModel.onAction(NewReminderAction.VetReasonSelected(VetReason.GENERAL))
        viewModel.fillDateTime(time = LocalTime.of(9, 0))

        viewModel.events.test {
            viewModel.onAction(NewReminderAction.Save)
            testScheduler.advanceUntilIdle()
            assertEquals(NewReminderEvent.Saved, awaitItem())
        }
        val draft = reminderRepository.created.single().second
        assertEquals(secondPet.id, draft.petId)
        assertEquals(ReminderKind.VET, draft.kind)
        assertEquals(VetReason.GENERAL, draft.vetReason)
        assertNull(draft.vaccineType)
        assertNull(draft.customTitle)
        assertEquals(LocalDateTime.of(tomorrow, LocalTime.of(9, 0)), draft.dateTime)
    }

    @Test
    fun `guardar un recordatorio Otro lleva el nombre`() = runTest {
        petRepository.pets.value = AppResult.Success(listOf(TestPet))
        val viewModel = viewModel(this, ReminderKind.OTHER)
        viewModel.onAction(NewReminderAction.TitleChanged("Baño"))
        viewModel.fillDateTime()

        viewModel.onAction(NewReminderAction.Save)
        testScheduler.advanceUntilIdle()

        assertEquals("Baño", reminderRepository.created.single().second.customTitle)
    }

    @Test
    fun `una hora que ya paso muestra su aviso y no guarda`() = runTest {
        petRepository.pets.value = AppResult.Success(listOf(TestPet))
        val viewModel = viewModel(this, ReminderKind.VACCINE)
        viewModel.onAction(NewReminderAction.VaccineSelected(VaccineType.RABIES))
        viewModel.fillDateTime(date = LocalDate.of(2026, 11, 1), time = LocalTime.of(8, 0))

        viewModel.onAction(NewReminderAction.Save)

        assertEquals(ValidationError.ReminderInPast.toFieldFeedback(), viewModel.uiState.value.dateTimeFeedback)
        assertTrue(reminderRepository.created.isEmpty())
    }

    @Test
    fun `un nombre de mas de 50 caracteres muestra su aviso`() = runTest {
        petRepository.pets.value = AppResult.Success(listOf(TestPet))
        val viewModel = viewModel(this, ReminderKind.OTHER)
        viewModel.onAction(NewReminderAction.TitleChanged("a".repeat(51)))
        viewModel.fillDateTime()

        viewModel.onAction(NewReminderAction.Save)

        assertEquals(ValidationError.ReminderTitleTooLong.toFieldFeedback(), viewModel.uiState.value.titleFeedback)
        assertTrue(reminderRepository.created.isEmpty())
    }

    @Test
    fun `sin conexion avisa y no guarda`() = runTest {
        networkMonitor.isOnline.value = false
        petRepository.pets.value = AppResult.Success(listOf(TestPet))
        val viewModel = viewModel(this, ReminderKind.VACCINE)
        viewModel.onAction(NewReminderAction.VaccineSelected(VaccineType.PARVOVIRUS))
        viewModel.fillDateTime()

        viewModel.events.test {
            viewModel.onAction(NewReminderAction.Save)
            testScheduler.advanceUntilIdle()
            assertEquals(NewReminderEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(reminderRepository.created.isEmpty())
    }
}
