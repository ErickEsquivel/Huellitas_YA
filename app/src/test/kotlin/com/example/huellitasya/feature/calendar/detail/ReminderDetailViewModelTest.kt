package com.example.huellitasya.feature.calendar.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.DeleteReminderUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveReminderUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRemindersUseCase
import com.example.huellitasya.core.domain.usecase.UpdateReminderUseCase
import com.example.huellitasya.core.domain.validation.ValidateReminderUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeReminderRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestReminder
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toFieldFeedback
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet)) }

    /** "Ahora" es el 1 de septiembre de 2026: el recordatorio de prueba (23 de septiembre) es futuro. */
    private val clock = Clock.fixed(
        LocalDateTime.of(2026, 9, 1, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
    )
    private val reminderRepository = FakeReminderRepository().apply { reminders.value = AppResult.Success(listOf(TestReminder)) }
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope): ReminderDetailViewModel {
        val observeReminders = ObserveRemindersUseCase(authRepository, reminderRepository)
        return ReminderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ReminderDetailViewModel.REMINDER_ID_ARG to TestReminder.id)),
            observeReminder = ObserveReminderUseCase(observeReminders),
            observePets = ObservePetsUseCase(authRepository, petRepository),
            updateReminder = UpdateReminderUseCase(authRepository, reminderRepository, appScope),
            deleteReminder = DeleteReminderUseCase(authRepository, reminderRepository, appScope),
            validateReminder = ValidateReminderUseCase(),
            networkMonitor = networkMonitor,
            clock = clock,
        )
    }

    @Test
    fun `muestra el recordatorio con su mascota y sin cambios`() = runTest {
        val state = viewModel(this).uiState.value

        assertEquals(TestReminder, state.reminder)
        assertEquals(TestPet, state.pet)
        assertEquals(TestReminder.dateTime.toLocalDate(), state.date)
        assertEquals(TestReminder.dateTime.toLocalTime(), state.time)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `cambiar la hora habilita actualizar y guardar vuelve atras`() = runTest {
        val viewModel = viewModel(this)
        viewModel.onAction(ReminderDetailAction.TimeChanged(LocalTime.of(15, 30)))
        assertTrue(viewModel.uiState.value.hasChanges)

        viewModel.events.test {
            viewModel.onAction(ReminderDetailAction.Update)
            testScheduler.advanceUntilIdle()
            assertEquals(ReminderDetailEvent.Close, awaitItem())
        }
        assertEquals(listOf(TestReminder.id to LocalDateTime.of(2026, 9, 23, 15, 30)), reminderRepository.updated)
    }

    @Test
    fun `una fecha pasada muestra su aviso y no guarda`() = runTest {
        val viewModel = viewModel(this)
        viewModel.onAction(ReminderDetailAction.DateChanged(LocalDate.of(2026, 8, 20)))

        viewModel.onAction(ReminderDetailAction.Update)

        assertEquals(ValidationError.ReminderInPast.toFieldFeedback(), viewModel.uiState.value.dateTimeFeedback)
        assertTrue(reminderRepository.updated.isEmpty())
    }

    @Test
    fun `eliminar pide confirmacion borra y vuelve una sola vez`() = runTest {
        val viewModel = viewModel(this)
        viewModel.onAction(ReminderDetailAction.DeleteClick)
        assertTrue(viewModel.uiState.value.showDeleteDialog)

        viewModel.events.test {
            viewModel.onAction(ReminderDetailAction.ConfirmDelete)
            testScheduler.advanceUntilIdle()
            assertEquals(ReminderDetailEvent.Close, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(TestReminder.id), reminderRepository.deleted)
    }

    @Test
    fun `sin conexion no elimina y avisa`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(this)

        viewModel.events.test {
            viewModel.onAction(ReminderDetailAction.ConfirmDelete)
            testScheduler.advanceUntilIdle()
            assertEquals(ReminderDetailEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(reminderRepository.deleted.isEmpty())
    }

    @Test
    fun `si se elimina desde otro dispositivo vuelve atras`() = runTest {
        val viewModel = viewModel(this)

        viewModel.events.test {
            reminderRepository.reminders.value = AppResult.Success(emptyList())
            assertEquals(ReminderDetailEvent.Close, awaitItem())
        }
    }
}
