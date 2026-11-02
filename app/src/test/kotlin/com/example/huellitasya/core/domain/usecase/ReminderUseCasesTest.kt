package com.example.huellitasya.core.domain.usecase

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.ReminderDraft
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.domain.repository.ScheduledReminder
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeReminderNotifier
import com.example.huellitasya.core.testing.FakeReminderRepository
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestReminder
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.ZoneId
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

    @Test
    fun `observar un recordatorio avisa NotFound cuando se elimina`() = runTest {
        authRepository.currentUser.value = TestUser
        reminderRepository.reminders.value = AppResult.Success(listOf(TestReminder))

        ObserveReminderUseCase(observeReminders)(TestReminder.id).test {
            assertEquals(AppResult.Success(TestReminder), awaitItem())
            reminderRepository.reminders.value = AppResult.Success(emptyList())
            assertEquals(AppResult.Failure(DataError.NotFound), awaitItem())
        }
    }

    @Test
    fun `actualizar y eliminar llegan al repositorio`() = runTest {
        authRepository.currentUser.value = TestUser
        reminderRepository.reminders.value = AppResult.Success(listOf(TestReminder))
        val newDateTime = LocalDateTime.of(2026, 11, 20, 8, 30)

        assertEquals(AppResult.Success(Unit), UpdateReminderUseCase(authRepository, reminderRepository, backgroundScope)(TestReminder.id, newDateTime))
        assertEquals(AppResult.Success(Unit), DeleteReminderUseCase(authRepository, reminderRepository, backgroundScope)(TestReminder.id))

        assertEquals(listOf(TestReminder.id to newDateTime), reminderRepository.updated)
        assertEquals(listOf(TestReminder.id), reminderRepository.deleted)
    }

    @Test
    fun `la sincronizacion programa los futuros con el nombre de su mascota y cancela todo al cerrar sesion`() = runTest {
        authRepository.currentUser.value = TestUser
        val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet)) }
        val orphan = TestReminder.copy(id = "orphan", petId = "pet-borrada")
        reminderRepository.reminders.value = AppResult.Success(listOf(past, TestReminder, orphan))
        val notifier = FakeReminderNotifier()
        val clock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
        val sync = SyncReminderNotificationsUseCase(authRepository, petRepository, reminderRepository, notifier, clock)

        backgroundScope.launch { sync() }
        runCurrent()
        assertEquals(listOf(ScheduledReminder(TestReminder, TestPet.name)), notifier.syncs.last())

        reminderRepository.reminders.value = AppResult.Success(emptyList())
        runCurrent()
        assertTrue(notifier.syncs.last().isEmpty())

        reminderRepository.reminders.value = AppResult.Success(listOf(TestReminder))
        runCurrent()
        authRepository.currentUser.value = null
        runCurrent()
        assertTrue(notifier.syncs.last().isEmpty())
    }
}
