package com.example.huellitasya.core.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.huellitasya.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Publica el aviso de un recordatorio a su hora (spec 007): "Vacuna contra la rabia" con
 * "Maximiliano Tercero · 11:00 AM". Tocarlo abre la app en el Calendario. Sin el permiso de
 * notificaciones (Android 13+) no muestra nada.
 */
internal class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val id = inputData.getString(KEY_ID) ?: return Result.success()
        val title = titleOf(context)
        val time = inputData.getString(KEY_TIME)?.let { LocalTime.parse(it).format(TimeFormatter) }.orEmpty()
        val text = listOfNotNull(inputData.getString(KEY_PET_NAME), time.ifEmpty { null }).joinToString(" · ")

        ensureChannel(context)
        val openCalendar = Intent()
            .setClassName(context, MAIN_ACTIVITY)
            .putExtra(EXTRA_OPEN_CALENDAR, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            openCalendar,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_calendar)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        } catch (e: SecurityException) {
            // El permiso se retiró entre la comprobación y el aviso: no hay nada más que hacer.
        }
        return Result.success()
    }

    /** Mismo título que las tarjetas del Calendario: el nombre escrito o el de la vacuna o el motivo. */
    private fun titleOf(context: Context): String {
        inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() }?.let { return it }
        val res = when (inputData.getString(KEY_KIND)) {
            "vaccine" -> when (inputData.getString(KEY_SUBTYPE)) {
                "rabies" -> R.string.reminder_title_rabies
                "parvovirus" -> R.string.reminder_title_parvovirus
                "leptospirosis" -> R.string.reminder_title_leptospirosis
                else -> R.string.reminder_title_vaccine
            }
            "vet" -> when (inputData.getString(KEY_SUBTYPE)) {
                "general" -> R.string.reminder_title_general
                "deworming" -> R.string.reminder_title_deworming
                "grooming" -> R.string.reminder_title_grooming
                "surgery" -> R.string.reminder_title_surgery
                else -> R.string.reminder_title_vet
            }
            else -> R.string.reminder_title_other
        }
        return context.getString(res)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_KIND = "kind"
        const val KEY_SUBTYPE = "subtype"
        const val KEY_TITLE = "title"
        const val KEY_PET_NAME = "petName"
        const val KEY_TIME = "time"

        /** Extra del Intent del aviso: `MainActivity` abre el Calendario. */
        const val EXTRA_OPEN_CALENDAR = "com.example.huellitasya.OPEN_CALENDAR"

        private const val CHANNEL_ID = "reminders"
        private const val MAIN_ACTIVITY = "com.example.huellitasya.MainActivity"
        private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    }
}
