package com.example.huellitasya.feature.calendar.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.DestructiveRed
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenCard
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.DeleteConfirmDialog
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasDatePickerDialog
import com.example.huellitasya.core.designsystem.components.HuellitasTimePickerDialog
import com.example.huellitasya.core.designsystem.components.IconCardClickableField
import com.example.huellitasya.core.designsystem.components.ProgressOverlay
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.calendar.components.ReminderFieldDateFormatter
import com.example.huellitasya.feature.calendar.components.ReminderTimeFormatter
import com.example.huellitasya.feature.calendar.components.dateLabelRes
import com.example.huellitasya.feature.calendar.components.displayTitle
import com.example.huellitasya.feature.calendar.components.iconRes
import com.example.huellitasya.feature.calendar.components.timeLabelRes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
internal fun ReminderDetailRoute(
    onBackClick: () -> Unit,
    onClosed: () -> Unit,
    viewModel: ReminderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ReminderDetailEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            ReminderDetailEvent.Close -> onClosed()
        }
    }

    ReminderDetailScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}

private val CardShape = RoundedCornerShape(20.dp)

/**
 * Detalle de un recordatorio (Figma 3376:1050): ícono y título, la tarjeta verde con la mascota y
 * los campos Fecha y Hora editables, "Actualizar información" y "Eliminar recordatorio" (o
 * "Eliminar cita"). Siempre editable: la versión de solo lectura de Figma no se implementa (spec 007).
 */
@Composable
internal fun ReminderDetailScreen(
    state: ReminderDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onAction: (ReminderDetailAction) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val reminder = state.reminder
    val overlayText = stringResource(if (state.isDeleting) R.string.reminder_detail_deleting else R.string.reminder_detail_updating)

    ProgressOverlay(isVisible = state.isBusy, text = overlayText) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            CurvedBackground(modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.55f))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    HuellitasBackButton(onClick = onBackClick, contentDescription = stringResource(R.string.reminder_detail_back))
                    if (reminder != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(reminder.kind.iconRes),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.Black),
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reminder.displayTitle(),
                            color = Color.Black,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (reminder != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(GreenCard)
                            .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Text(
                            text = state.pet?.name.orEmpty(),
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                        PetPhoto(pet = state.pet)
                        IconCardClickableField(
                            valueText = state.date?.format(ReminderFieldDateFormatter),
                            onClick = { showDatePicker = true },
                            label = stringResource(reminder.kind.dateLabelRes),
                            icon = painterResource(R.drawable.ic_field_calendar),
                            iconSize = 40.dp,
                            status = state.dateTimeFeedback.statusOrDefault,
                        )
                        IconCardClickableField(
                            valueText = state.time?.format(ReminderTimeFormatter),
                            onClick = { showTimePicker = true },
                            label = stringResource(reminder.kind.timeLabelRes),
                            icon = painterResource(R.drawable.ic_calendar_clock),
                            iconSize = 36.dp,
                            status = state.dateTimeFeedback.statusOrDefault,
                            message = state.dateTimeFeedback?.message?.asString(),
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    HuellitasButton(
                        text = stringResource(R.string.reminder_detail_update),
                        onClick = { onAction(ReminderDetailAction.Update) },
                        enabled = state.hasChanges,
                        containerColor = if (state.hasChanges) GreenPrimary else ButtonIncomplete,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).width(279.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HuellitasButton(
                        text = stringResource(
                            if (reminder.kind == ReminderKind.VET) R.string.reminder_detail_delete_appointment else R.string.reminder_detail_delete,
                        ),
                        onClick = { onAction(ReminderDetailAction.DeleteClick) },
                        containerColor = DestructiveRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).width(279.dp),
                    )
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }

    if (state.showDeleteDialog) {
        DeleteConfirmDialog(
            title = stringResource(R.string.reminder_delete_title),
            message = stringResource(R.string.reminder_delete_message),
            confirmText = stringResource(R.string.reminder_delete_confirm),
            closeDescription = stringResource(R.string.reminder_delete_close),
            imageDescription = stringResource(R.string.reminder_delete_image_description),
            onConfirm = { onAction(ReminderDetailAction.ConfirmDelete) },
            onDismiss = { onAction(ReminderDetailAction.DismissDelete) },
        )
    }
    if (showDatePicker) {
        val today = LocalDate.now()
        HuellitasDatePickerDialog(
            initialDate = state.date ?: today,
            minDate = today,
            maxDate = today.plusYears(5),
            confirmText = stringResource(R.string.picker_accept),
            cancelText = stringResource(R.string.picker_cancel),
            onDateSelected = { onAction(ReminderDetailAction.DateChanged(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        HuellitasTimePickerDialog(
            initialTime = state.time,
            confirmText = stringResource(R.string.picker_accept),
            cancelText = stringResource(R.string.picker_cancel),
            onTimeSelected = { onAction(ReminderDetailAction.TimeChanged(it)) },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** Foto de 177 dp sobre el círculo verde claro (Figma 3376:1067); sin foto, el ícono de mascota. */
@Composable
private fun PetPhoto(pet: Pet?) {
    Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(GreenBackground), contentAlignment = Alignment.Center) {
        val photoUrl = pet?.photoUrl
        if (photoUrl == null) {
            Image(painter = painterResource(R.drawable.ic_pets_breed), contentDescription = null, modifier = Modifier.size(110.dp))
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = stringResource(R.string.reminder_detail_pet_photo, pet.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(177.dp).clip(CircleShape),
            )
        }
    }
}

private val FieldFeedback?.statusOrDefault: FieldStatus get() = this?.status ?: FieldStatus.Default

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ReminderDetailScreenPreview() {
    val reminder = Reminder("1", "pet-1", ReminderKind.VACCINE, VaccineType.RABIES, null, null, LocalDateTime.of(2026, 11, 24, 9, 0))
    HuellitasYATheme {
        ReminderDetailScreen(
            state = ReminderDetailUiState(
                reminder = reminder,
                pet = Pet("pet-1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.of(2026, 6, 6), PetSex.MALE, 5.0, null),
                date = reminder.dateTime.toLocalDate(),
                time = reminder.dateTime.toLocalTime(),
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}
