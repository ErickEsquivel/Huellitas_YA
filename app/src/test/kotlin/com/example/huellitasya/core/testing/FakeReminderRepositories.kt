package com.example.huellitasya.core.testing

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.ReminderNotifier
import com.example.huellitasya.core.domain.repository.ReminderRepository
import com.example.huellitasya.core.domain.repository.ScheduledReminder
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderDraft
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

val TestReminder = Reminder(
    id = "reminder-1",
    petId = TestPet.id,
    kind = ReminderKind.VACCINE,
    vaccineType = VaccineType.RABIES,
    vetReason = null,
    customTitle = null,
    dateTime = LocalDateTime.of(2026, 9, 23, 11, 0),
)

/**
 * [ReminderRepository] en memoria. [reminders] hace de colección `users/{uid}/reminders`: crear,
 * editar o borrar la actualiza, como haría el listener de Firestore.
 */
class FakeReminderRepository : ReminderRepository {
    val reminders = MutableStateFlow<AppResult<List<Reminder>, DataError>>(AppResult.Success(emptyList()))
    var writeResult: AppResult<Unit, DataError> = AppResult.Success(Unit)
    val created = mutableListOf<Pair<String, ReminderDraft>>()
    val updated = mutableListOf<Pair<String, LocalDateTime>>()
    val deleted = mutableListOf<String>()
    private var nextId = 1

    override fun observeReminders(uid: String): Flow<AppResult<List<Reminder>, DataError>> = reminders

    override fun newReminderId(uid: String): String = "reminder-${nextId++}"

    override suspend fun createReminder(uid: String, reminderId: String, draft: ReminderDraft): AppResult<Unit, DataError> {
        created += reminderId to draft
        edit { current ->
            val reminder = Reminder(reminderId, draft.petId, draft.kind, draft.vaccineType, draft.vetReason, draft.customTitle, draft.dateTime)
            (current + reminder).sortedBy { it.dateTime }
        }
        return writeResult
    }

    override suspend fun updateDateTime(uid: String, reminderId: String, dateTime: LocalDateTime): AppResult<Unit, DataError> {
        updated += reminderId to dateTime
        edit { current -> current.map { if (it.id == reminderId) it.copy(dateTime = dateTime) else it }.sortedBy { it.dateTime } }
        return writeResult
    }

    override suspend fun deleteReminder(uid: String, reminderId: String): AppResult<Unit, DataError> {
        deleted += reminderId
        edit { current -> current.filterNot { it.id == reminderId } }
        return writeResult
    }

    private fun edit(change: (List<Reminder>) -> List<Reminder>) {
        val current = (reminders.value as? AppResult.Success)?.data ?: return
        if (writeResult is AppResult.Success) reminders.value = AppResult.Success(change(current))
    }
}

class FakeReminderNotifier : ReminderNotifier {
    /** Cada llamada a [sync], en orden. */
    val syncs = mutableListOf<List<ScheduledReminder>>()

    override fun sync(reminders: List<ScheduledReminder>) {
        syncs += reminders
    }
}
