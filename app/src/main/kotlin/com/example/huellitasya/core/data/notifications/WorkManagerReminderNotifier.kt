package com.example.huellitasya.core.data.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.huellitasya.core.data.mapper.subtypeValue
import com.example.huellitasya.core.data.mapper.wireValue
import com.example.huellitasya.core.domain.repository.ReminderNotifier
import com.example.huellitasya.core.domain.repository.ScheduledReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Avisos con WorkManager (spec 007): un trabajo único `reminder-<id>` por recordatorio, con espera
 * hasta su hora. Sobrevive al reinicio del teléfono y no necesita el permiso de alarmas exactas; en
 * ahorro de batería puede llegar unos minutos tarde.
 */
internal class WorkManagerReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) : ReminderNotifier {

    override fun sync(reminders: List<ScheduledReminder>) {
        val workManager = WorkManager.getInstance(context)
        // WorkManager ejecuta sus operaciones en orden: primero se cancela todo y luego se programa
        // la lista actual, así no sobrevive el aviso de un recordatorio eliminado o cambiado.
        workManager.cancelAllWorkByTag(TAG)
        val now = LocalDateTime.now(clock)
        reminders.forEach { (reminder, petName) ->
            val delay = Duration.between(now, reminder.dateTime)
            if (delay.isNegative) return@forEach
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_ID to reminder.id,
                        ReminderWorker.KEY_KIND to reminder.kind.wireValue(),
                        ReminderWorker.KEY_SUBTYPE to subtypeValue(reminder.vaccineType, reminder.vetReason),
                        ReminderWorker.KEY_TITLE to reminder.customTitle,
                        ReminderWorker.KEY_PET_NAME to petName,
                        ReminderWorker.KEY_TIME to reminder.dateTime.toLocalTime().toString(),
                    ),
                )
                .build()
            workManager.enqueueUniqueWork("reminder-${reminder.id}", ExistingWorkPolicy.REPLACE, request)
        }
    }

    private companion object {
        const val TAG = "reminder"
    }
}
