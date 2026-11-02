package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/** Recordatorios del usuario en `users/{uid}/reminders`, de todas sus mascotas (spec 007). */
interface ReminderRepository {
    /** Todos los recordatorios, del más próximo al más lejano; sin red, desde la caché. */
    fun observeReminders(uid: String): Flow<AppResult<List<Reminder>, DataError>>

    /** Id para un recordatorio nuevo. Se genera antes de guardarlo para programar su aviso con el mismo id. */
    fun newReminderId(uid: String): String

    suspend fun createReminder(uid: String, reminderId: String, draft: ReminderDraft): AppResult<Unit, DataError>

    /** Lo único editable de un recordatorio es su fecha y hora. */
    suspend fun updateDateTime(uid: String, reminderId: String, dateTime: LocalDateTime): AppResult<Unit, DataError>

    suspend fun deleteReminder(uid: String, reminderId: String): AppResult<Unit, DataError>
}
