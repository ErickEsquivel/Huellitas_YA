package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.map
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.PetRepository
import com.example.huellitasya.core.domain.repository.ReminderNotifier
import com.example.huellitasya.core.domain.repository.ReminderRepository
import com.example.huellitasya.core.domain.repository.ScheduledReminder
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
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

/** Un recordatorio en vivo (detalle). `NotFound` si se eliminó, aquí o desde otro dispositivo. */
class ObserveReminderUseCase @Inject constructor(
    private val observeReminders: ObserveRemindersUseCase,
) {
    operator fun invoke(reminderId: String): Flow<AppResult<Reminder, DataError>> =
        observeReminders().map { result ->
            when (result) {
                is AppResult.Success -> result.data.firstOrNull { it.id == reminderId }
                    ?.let { AppResult.Success(it) } ?: AppResult.Failure(DataError.NotFound)
                is AppResult.Failure -> result
            }
        }
}

/** Cambia la fecha y la hora, lo único editable de un recordatorio. Corre en [ApplicationScope]. */
class UpdateReminderUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(reminderId: String, dateTime: LocalDateTime): AppResult<Unit, DataError> = appScope.async {
        val uid = authRepository.currentUser.first()?.uid
            ?: return@async AppResult.Failure(DataError.PermissionDenied)
        reminderRepository.updateDateTime(uid, reminderId, dateTime)
    }.await()
}

/** Elimina un recordatorio. Corre en [ApplicationScope]: el detalle vuelve al Calendario al terminar. */
class DeleteReminderUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(reminderId: String): AppResult<Unit, DataError> = appScope.async {
        val uid = authRepository.currentUser.first()?.uid
            ?: return@async AppResult.Failure(DataError.PermissionDenied)
        reminderRepository.deleteReminder(uid, reminderId)
    }.await()
}

/**
 * Mantiene los avisos del teléfono al día mientras la app está abierta: con cada cambio de los
 * recordatorios o de las mascotas programa los futuros y cancela los demás; sin sesión, los cancela
 * todos. Así crear, editar, eliminar (aquí o en otro dispositivo) y eliminar una mascota quedan
 * cubiertos por un solo camino. No termina: se lanza una vez desde `MainViewModel`.
 */
class SyncReminderNotificationsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val reminderRepository: ReminderRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke() {
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    combine(petRepository.observePets(user.uid), reminderRepository.observeReminders(user.uid)) { pets, reminders ->
                        // Con un error de lectura se conservan los avisos que ya había.
                        if (pets !is AppResult.Success || reminders !is AppResult.Success) return@combine null
                        val names = pets.data.associate { it.id to it.name }
                        val now = LocalDateTime.now(clock)
                        reminders.data.mapNotNull { reminder ->
                            val petName = names[reminder.petId] ?: return@mapNotNull null
                            ScheduledReminder(reminder, petName).takeIf { reminder.dateTime.isAfter(now) }
                        }
                    }.filterNotNull()
                }
            }
            .collect { notifier.sync(it) }
    }
}
