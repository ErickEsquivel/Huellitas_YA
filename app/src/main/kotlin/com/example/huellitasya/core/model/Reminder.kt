package com.example.huellitasya.core.model

import java.time.LocalDateTime

/** Tipo de recordatorio, según la opción elegida en "¿Qué deseas programar?…" (spec 007). */
enum class ReminderKind { VACCINE, VET, OTHER }

/** Vacuna de un recordatorio de tipo [ReminderKind.VACCINE]. `OTHER` pide además un nombre. */
enum class VaccineType { RABIES, PARVOVIRUS, LEPTOSPIROSIS, OTHER }

/** Motivo de una cita de tipo [ReminderKind.VET]. `OTHER` pide además un nombre. */
enum class VetReason { GENERAL, DEWORMING, GROOMING, SURGERY, OTHER }

/** Recordatorio guardado en `users/{uid}/reminders/{reminderId}` (spec 007). */
data class Reminder(
    val id: String,
    val petId: String,
    val kind: ReminderKind,
    val vaccineType: VaccineType?,
    val vetReason: VetReason?,
    /** Nombre escrito por el usuario: en [ReminderKind.OTHER] y en los subtipos `OTHER`. */
    val customTitle: String?,
    val dateTime: LocalDateTime,
)

/** Datos ya validados del formulario "Nuevo recordatorio". */
data class ReminderDraft(
    val petId: String,
    val kind: ReminderKind,
    val vaccineType: VaccineType?,
    val vetReason: VetReason?,
    val customTitle: String?,
    val dateTime: LocalDateTime,
)

/** Máximo de caracteres del nombre de un recordatorio (mismo límite que `firestore.rules`). */
const val MAX_REMINDER_TITLE_LENGTH = 50
