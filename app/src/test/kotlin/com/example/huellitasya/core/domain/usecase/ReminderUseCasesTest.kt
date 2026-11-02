package com.example.huellitasya.core.domain.usecase

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.ReminderDraft
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeReminderRepository
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestReminder
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val reminderRepository = FakeReminderRepository()
    private val observeReminders = ObserveRemindersUseCase(authRepository, reminderRepository)

    private val past = TestReminder.copy(id = "reminder-0", dateTime = LocalDateTime.of(2026, 9, 1, 9, 0))
    private val now = LocalDateTime.of(2026, 9, 18, 12, 0)

    @Test
    fun `sin sesion no se observan recordatorios`() = runTest {
        observeReminders().test {
            expectNoEvents()
        }
    }

    @Test
    fun `los proximos recordatorios dejan fuera los que ya pasaron`() = runTest {
        authRepository.currentUser.value = TestUser
        reminderRepository.reminders.value = AppResult.Success(listOf(past, TestReminder))

        ObserveUpcomingRemindersUseCase(observeReminders)(now = { now }).test {
            assertEquals(AppResult.Success(listOf(TestReminder)), awaitItem())
        }
    }

    @Test
    fun `un recordatorio de la hora exacta todavia es proximo`() = runTest {
        authRepository.currentUser.value = TestUser
        reminderRepository.reminders.value = AppResult.Success(listOf(TestReminder))

        ObserveUpcomingRemindersUseCase(observeReminders)(now = { TestReminder.dateTime }).test {
            assertEquals(AppResult.Success(listOf(TestReminder)), awaitItem())
        }
    }

    @Test
    fun `los errores de lectura se propagan`() = runTest {
        authRepository.currentUser.value = TestUser
        reminderRepository.reminders.value = AppResult.Failure(DataError.PermissionDenied)

        ObserveUpcomingRemindersUseCase(observeReminders)(now = { now }).test {
            assertEquals(AppResult.Failure(DataError.PermissionDenied), awaitItem())
        }
    }

    @Test
    fun `crear un recordatorio lo guarda con el nombre recortado y devuelve su id`() = runTest {
        authRepository.currentUser.value = TestUser
        val draft = ReminderDraft(TestPet.id, ReminderKind.OTHER, null, null, "  Baño  ", LocalDateTime.of(2026, 11, 15, 10, 0))

        val result = CreateReminderUseCase(authRepository, reminderRepository, backgroundScope)(draft)

        assertEquals(AppResult.Success("reminder-1"), result)
        assertEquals("reminder-1" to draft.copy(customTitle = "Baño"), reminderRepository.created.single())
    }

    @Test
    fun `sin sesion no se crea`() = runTest {
        val draft = ReminderDraft(TestPet.id, ReminderKind.OTHER, null, null, "Baño", LocalDateTime.of(2026, 11, 15, 10, 0))

        val result = CreateReminderUseCase(authRepository, reminderRepository, backgroundScope)(draft)

        assertEquals(AppResult.Failure(DataError.PermissionDenied), result)
    }
}
