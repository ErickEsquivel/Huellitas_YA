package com.example.huellitasya.feature.calendar.notifications

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRemindersUseCase
import com.example.huellitasya.core.domain.usecase.ObserveUpcomingRemindersUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeReminderRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestReminder
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val secondPet = TestPet.copy(id = "pet-2", name = "Maximiliano Cuarto")
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet, secondPet)) }
    private val reminderRepository = FakeReminderRepository()

    /** "Ahora" es el 18 de septiembre de 2026. */
    private val clock = Clock.fixed(
        LocalDateTime.of(2026, 9, 18, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
    )

    private fun viewModel() = NotificationsViewModel(
        observePets = ObservePetsUseCase(authRepository, petRepository),
        observeUpcomingReminders = ObserveUpcomingRemindersUseCase(ObserveRemindersUseCase(authRepository, reminderRepository)),
        clock = clock,
    )

    @Test
    fun `lista los proximos con el color de su mascota y sin los pasados ni los de mascotas eliminadas`() = runTest {
        val past = TestReminder.copy(id = "past", dateTime = LocalDateTime.of(2026, 9, 1, 9, 0))
        val second = TestReminder.copy(id = "second", petId = secondPet.id, dateTime = LocalDateTime.of(2026, 9, 24, 9, 0))
        val orphan = TestReminder.copy(id = "orphan", petId = "pet-borrada")
        reminderRepository.reminders.value = AppResult.Success(listOf(past, TestReminder, second, orphan))

        val state = viewModel().uiState.value

        assertEquals(listOf(NotificationItem(TestReminder, 0), NotificationItem(second, 1)), state.items)
        assertFalse(state.isLoading)
    }

    @Test
    fun `sin recordatorios la lista queda vacia y cargada`() = runTest {
        val state = viewModel().uiState.value

        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `un error de lectura se avisa`() = runTest {
        reminderRepository.reminders.value = AppResult.Failure(DataError.Network)
        val viewModel = viewModel()

        viewModel.events.test {
            assertEquals(NotificationsEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
    }
}
