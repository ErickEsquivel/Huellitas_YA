package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.model.Reminder

/** Un recordatorio futuro con el nombre de su mascota, para el texto del aviso. */
data class ScheduledReminder(val reminder: Reminder, val petName: String)

/** Avisos del teléfono a la hora de cada recordatorio (spec 007). */
interface ReminderNotifier {
    /**
     * Deja programados exactamente estos avisos: reprograma los que cambiaron y cancela los que ya no
     * están. Una lista vacía cancela todos (al cerrar sesión).
     */
    fun sync(reminders: List<ScheduledReminder>)
}
