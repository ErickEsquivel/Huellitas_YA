package com.example.huellitasya.feature.calendar.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.example.huellitasya.R
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.model.VetReason
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Título que se muestra: el de la vacuna o el motivo, o el nombre escrito por el usuario (spec 007). */
@Composable
internal fun Reminder.displayTitle(): String {
    customTitle?.takeIf { it.isNotBlank() }?.let { return it }
    val res = when (kind) {
        ReminderKind.VACCINE -> when (vaccineType) {
            VaccineType.RABIES -> R.string.reminder_title_rabies
            VaccineType.PARVOVIRUS -> R.string.reminder_title_parvovirus
            VaccineType.LEPTOSPIROSIS -> R.string.reminder_title_leptospirosis
            VaccineType.OTHER, null -> R.string.reminder_title_vaccine
        }
        ReminderKind.VET -> when (vetReason) {
            VetReason.GENERAL -> R.string.reminder_title_general
            VetReason.DEWORMING -> R.string.reminder_title_deworming
            VetReason.GROOMING -> R.string.reminder_title_grooming
            VetReason.SURGERY -> R.string.reminder_title_surgery
            VetReason.OTHER, null -> R.string.reminder_title_vet
        }
        ReminderKind.OTHER -> R.string.reminder_title_other
    }
    return stringResource(res)
}

/** Ícono del tipo, en negro: jeringa, corazón o lista (Figma 3241:1717). */
@get:DrawableRes
internal val ReminderKind.iconRes: Int
    get() = when (this) {
        ReminderKind.VACCINE -> R.drawable.ic_pets_record_vaccines
        ReminderKind.VET -> R.drawable.ic_pets_record_history
        ReminderKind.OTHER -> R.drawable.ic_calendar_other
    }

/** "11:00 AM", como en Figma. */
internal val ReminderTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

/** "23 de sep. de 2026". Las abreviaturas vienen de recursos: las de Java cambian según la versión ("sept."). */
@Composable
internal fun LocalDate.displayDate(): String =
    stringResource(R.string.calendar_date, dayOfMonth, stringArrayResource(R.array.calendar_months_short)[monthValue - 1], year)

@Composable
internal fun LocalDateTime.displayTime(): String = format(ReminderTimeFormatter)

/** "Septiembre 2026". */
@Composable
internal fun YearMonth.displayMonth(): String =
    stringResource(R.string.calendar_month_year, stringArrayResource(R.array.calendar_months)[monthValue - 1], year)

/** Nombre de la opción en "¿Qué deseas programar?…" y título de "Nuevo recordatorio". */
@get:StringRes
internal val ReminderKind.labelRes: Int
    get() = when (this) {
        ReminderKind.VACCINE -> R.string.reminder_kind_vaccine
        ReminderKind.VET -> R.string.reminder_kind_vet
        ReminderKind.OTHER -> R.string.reminder_kind_other
    }

@get:StringRes
internal val VaccineType.labelRes: Int
    get() = when (this) {
        VaccineType.RABIES -> R.string.vaccine_rabies
        VaccineType.PARVOVIRUS -> R.string.vaccine_parvovirus
        VaccineType.LEPTOSPIROSIS -> R.string.vaccine_leptospirosis
        VaccineType.OTHER -> R.string.option_other
    }

/** Las vacunas llevan el escudo (Figma 3362:1590) y "Otros", los tres puntos. */
@get:DrawableRes
internal val VaccineType.iconRes: Int
    get() = if (this == VaccineType.OTHER) R.drawable.ic_calendar_more else R.drawable.ic_calendar_vaccine

@get:StringRes
internal val VetReason.labelRes: Int
    get() = when (this) {
        VetReason.GENERAL -> R.string.vet_reason_general
        VetReason.DEWORMING -> R.string.vet_reason_deworming
        VetReason.GROOMING -> R.string.vet_reason_grooming
        VetReason.SURGERY -> R.string.vet_reason_surgery
        VetReason.OTHER -> R.string.option_other
    }

/** Íconos de los motivos (Figma 3241:1383-1410). */
@get:DrawableRes
internal val VetReason.iconRes: Int
    get() = when (this) {
        VetReason.GENERAL -> R.drawable.ic_calendar_general
        VetReason.DEWORMING -> R.drawable.ic_calendar_deworming
        VetReason.GROOMING -> R.drawable.ic_calendar_grooming
        VetReason.SURGERY -> R.drawable.ic_calendar_surgery
        VetReason.OTHER -> R.drawable.ic_calendar_more
    }

/** Etiquetas de los campos Fecha y Hora según el tipo (Figma: "Fecha de vacuna", "Hora de cita"…). */
@get:StringRes
internal val ReminderKind.dateLabelRes: Int
    get() = when (this) {
        ReminderKind.VACCINE -> R.string.reminder_date_vaccine
        ReminderKind.VET -> R.string.reminder_date_vet
        ReminderKind.OTHER -> R.string.reminder_date_other
    }

@get:StringRes
internal val ReminderKind.timeLabelRes: Int
    get() = when (this) {
        ReminderKind.VACCINE -> R.string.reminder_time_vaccine
        ReminderKind.VET -> R.string.reminder_time_vet
        ReminderKind.OTHER -> R.string.reminder_time_other
    }

/** "23 / 09 / 2026" en los campos de fecha (Figma 3362:1605). */
internal val ReminderFieldDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd  /  MM  /  yyyy")
