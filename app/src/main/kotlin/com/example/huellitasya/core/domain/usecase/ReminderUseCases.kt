package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.map
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.ReminderRepository
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

/** Todos los recordatorios del usuario con sesión, en vivo y ordenados por fecha (spec 007). */
class ObserveRemindersUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AppResult<List<Reminder>, DataError>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user -> if (user == null) emptyFlow() else reminderRepository.observeReminders(user.uid) }
}

/**
 * Los recordatorios que todavía no pasaron ("Próximos eventos" y Notificaciones). [now] se evalúa
 * con cada cambio de la lista, así que uno que ya pasó desaparece en la siguiente actualización.
 */
class ObserveUpcomingRemindersUseCase @Inject constructor(
    private val observeReminders: ObserveRemindersUseCase,
) {
    operator fun invoke(now: () -> LocalDateTime = LocalDateTime::now): Flow<AppResult<List<Reminder>, DataError>> =
        observeReminders().map { result ->
            result.map { reminders ->
                val current = now()
                reminders.filterNot { it.dateTime.isBefore(current) }
            }
        }
}

/**
 * Guarda un recordatorio nuevo y devuelve su id. Corre en [ApplicationScope]: "Nuevo recordatorio"
 * vuelve al Calendario al terminar.
 */
class CreateReminderUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(draft: ReminderDraft): AppResult<String, DataError> = appScope.async {
        val uid = authRepository.currentUser.first()?.uid
            ?: return@async AppResult.Failure(DataError.PermissionDenied)
        val id = reminderRepository.newReminderId(uid)
        val normalized = draft.copy(customTitle = draft.customTitle?.trim()?.takeIf { it.isNotEmpty() })
        reminderRepository.createReminder(uid, id, normalized).map { id }
    }.await()
}
