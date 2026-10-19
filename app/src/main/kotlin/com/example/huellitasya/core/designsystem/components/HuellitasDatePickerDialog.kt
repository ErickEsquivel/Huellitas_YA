package com.example.huellitasya.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Selector de fecha de nacimiento de Material 3 (Datos Personales y mascotas, specs 004 y 005).
 * Trabaja en UTC (así lo define `DatePicker`) para que la fecha elegida no se corra un día por la
 * zona horaria. Solo deja elegir entre [minDate] y hoy.
 *
 * @param clearText y [onClear]: botón "Borrar" para dejar la fecha sin registrar. Sin ellos no se muestra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuellitasDatePickerDialog(
    initialDate: LocalDate?,
    minDate: LocalDate,
    confirmText: String,
    cancelText: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    clearText: String? = null,
    onClear: (() -> Unit)? = null,
) {
    val today = LocalDate.now()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = minDate.year..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcTimeMillis.toLocalDate()
                return !date.isAfter(LocalDate.now()) && !date.isBefore(minDate)
            }

            override fun isSelectableYear(year: Int): Boolean = year in minDate.year..LocalDate.now().year
        },
    )
    val colors = DatePickerDefaults.colors(containerColor = Color.White)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it.toLocalDate()) }
                    onDismiss()
                },
            ) { Text(confirmText) }
        },
        dismissButton = {
            Row {
                if (clearText != null && onClear != null) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        },
                    ) { Text(clearText) }
                }
                TextButton(onClick = onDismiss) { Text(cancelText) }
            }
        },
    ) {
        DatePicker(state = datePickerState, colors = colors)
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
