package com.example.huellitasya.feature.calendar

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRemindersUseCase
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VetReason
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val secondPet = TestPet.copy(id = "pet-2", name = "Maximiliano Cuarto")
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet, secondPet)) }
    private val reminderRepository = FakeReminderRepository()

    /** "Hoy" es el 18 de septiembre de 2026 a mediodía, como en Figma. */
    private val clock = Clock.fixed(
        LocalDateTime.of(2026, 9, 18, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
    )

    private val past = TestReminder.copy(id = "past", dateTime = LocalDateTime.of(2026, 9, 2, 9, 0))
    private val secondPetVet = TestReminder.copy(
        id = "vet",
        petId = secondPet.id,
        kind = ReminderKind.VET,
        vaccineType = null,
        vetReason = VetReason.GENERAL,
        dateTime = LocalDateTime.of(2026, 9, 24, 9, 0),
    )
    private val nextMonth = TestReminder.copy(id = "october", dateTime = LocalDateTime.of(2026, 10, 5, 10, 0))

    private fun viewModel() = CalendarViewModel(
        observePets = ObservePetsUseCase(authRepository, petRepository),
        observeReminders = ObserveRemindersUseCase(authRepository, reminderRepository),
        clock = clock,
    )

    @Test
    fun `abre en el mes de hoy y sin recordatorios muestra la tarjeta mas`() = runTest {
        val state = viewModel().uiState.value

        assertEquals(YearMonth.of(2026, 9), state.month)
        assertEquals(LocalDate.of(2026, 9, 18), state.today)
        assertTrue(state.upcoming.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `marca los dias del mes con el color de la mascota, tambien los pasados`() = runTest {
        reminderRepository.reminders.value = AppResult.Success(listOf(past, TestReminder, secondPetVet, nextMonth))

        val state = viewModel().uiState.value

        assertEquals(mapOf(2 to 0, 23 to 0, 24 to 1), state.dayMarks)
    }

    @Test
    fun `cambiar de mes recalcula las marcas`() = runTest {
        reminderRepository.reminders.value = AppResult.Success(listOf(TestReminder, nextMonth))
        val viewModel = viewModel()

        viewModel.onAction(CalendarAction.NextMonth)

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.month)
        assertEquals(mapOf(5 to 0), viewModel.uiState.value.dayMarks)
    }

    @Test
    fun `los proximos eventos se agrupan por mascota en orden de registro y sin los pasados`() = runTest {
        reminderRepository.reminders.value = AppResult.Success(listOf(past, TestReminder, secondPetVet, nextMonth))

        val upcoming = viewModel().uiState.value.upcoming

        assertEquals(listOf(TestPet.id, secondPet.id), upcoming.map { it.pet.id })
        assertEquals(listOf(0, 1), upcoming.map { it.colorIndex })
        assertEquals(listOf(TestReminder, nextMonth), upcoming[0].reminders)
        assertEquals(listOf(secondPetVet), upcoming[1].reminders)
    }

    @Test
    fun `los recordatorios de una mascota eliminada no se muestran`() = runTest {
        reminderRepository.reminders.value = AppResult.Success(listOf(TestReminder, secondPetVet))
        petRepository.pets.value = AppResult.Success(listOf(TestPet))

        val state = viewModel().uiState.value

        assertEquals(listOf(TestPet.id), state.upcoming.map { it.pet.id })
        assertEquals(mapOf(23 to 0), state.dayMarks)
    }

    @Test
    fun `un error de lectura se avisa`() = runTest {
        reminderRepository.reminders.value = AppResult.Failure(DataError.Network)
        val viewModel = viewModel()

        viewModel.events.test {
            assertEquals(CalendarEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `las semanas del mes empiezan el lunes`() {
        // Septiembre de 2026 empieza un martes: un hueco antes del 1.
        val weeks = monthWeeks(YearMonth.of(2026, 9))

        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), weeks.first())
        assertEquals(5, weeks.size)
        assertEquals(listOf(28, 29, 30, null, null, null, null), weeks.last())
    }
}
