package com.example.huellitasya.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import java.time.LocalTime

/**
 * Selector de hora de Material 3 (recordatorios, spec 007), hermano de [HuellitasDatePickerDialog].
 * Usa el reloj de 12 horas con AM/PM, como la hora que muestran las tarjetas ("11:00 AM").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuellitasTimePickerDialog(
    initialTime: LocalTime?,
    confirmText: String,
    cancelText: String,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = initialTime ?: LocalTime.of(9, 0)
    val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(state.hour, state.minute))
                    onDismiss()
                },
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelText) } },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = GreenBackground,
                    selectorColor = GreenPrimary,
                    timeSelectorSelectedContainerColor = GreenBackground,
                    periodSelectorSelectedContainerColor = GreenBackground,
                ),
            )
        },
    )
}

@Preview
@Composable
private fun HuellitasTimePickerDialogPreview() {
    HuellitasYATheme {
        HuellitasTimePickerDialog(LocalTime.of(11, 0), "Aceptar", "Cancelar", onTimeSelected = {}, onDismiss = {})
    }
}
