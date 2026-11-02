package com.example.huellitasya.feature.calendar.components

import androidx.annotation.DrawableRes
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
