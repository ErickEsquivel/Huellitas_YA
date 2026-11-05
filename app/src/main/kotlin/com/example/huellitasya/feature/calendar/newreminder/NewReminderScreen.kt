package com.example.huellitasya.feature.calendar.newreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenCard
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasBackBar
import com.example.huellitasya.core.designsystem.components.HuellitasBackBarHeight
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasDatePickerDialog
import com.example.huellitasya.core.designsystem.components.HuellitasTimePickerDialog
import com.example.huellitasya.core.designsystem.components.IconCardClickableField
import com.example.huellitasya.core.designsystem.components.IconCardTextField
import com.example.huellitasya.core.designsystem.components.ProgressOverlay
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.model.VetReason
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.calendar.components.ReminderFieldDateFormatter
import com.example.huellitasya.feature.calendar.components.ReminderTimeFormatter
import com.example.huellitasya.feature.calendar.components.dateLabelRes
import com.example.huellitasya.feature.calendar.components.iconRes
import com.example.huellitasya.feature.calendar.components.labelRes
import com.example.huellitasya.feature.calendar.components.timeLabelRes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun NewReminderRoute(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NewReminderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Se pide al guardar: sin permiso el recordatorio se guarda igual, solo que no hay aviso (spec 007).
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onSaved() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NewReminderEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            NewReminderEvent.Saved -> if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onSaved()
            }
        }
    }

    NewReminderScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}

private val CardShape = RoundedCornerShape(20.dp)

/**
 * "Nuevo recordatorio" (Figma: Vacunas 3362:1554, Veterinario 3367:1782, Otro 3367:2019): 1) la
 * mascota, 2) la vacuna, el motivo o el nombre, 3) la fecha y la hora, y "Agregar Recordatorio".
 */
@Composable
internal fun NewReminderScreen(
    state: NewReminderUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onAction: (NewReminderAction) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ProgressOverlay(isVisible = state.isSaving, text = stringResource(R.string.new_reminder_saving)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            CurvedBackground(modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.55f))
            HuellitasBackBar(
                onClick = onBackClick,
                contentDescription = stringResource(R.string.new_reminder_back),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = HuellitasBackBarHeight)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(state.kind.labelRes),
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 8.dp),
                )

                StepLabel(stringResource(R.string.new_reminder_step_pet), modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
                if (state.pets.isEmpty()) {
                    Text(text = stringResource(R.string.new_reminder_no_pets), color = GrayText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.pets.forEach { pet ->
                        PetChoiceCard(
                            pet = pet,
                            isSelected = pet.id == state.selectedPetId,
                            onClick = { onAction(NewReminderAction.PetSelected(pet.id)) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                StepTwo(state = state, onAction = onAction)

                StepLabel(stringResource(R.string.new_reminder_step_date), modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconCardClickableField(
                        valueText = state.date?.format(ReminderFieldDateFormatter),
                        onClick = {
                            focusManager.clearFocus()
                            showDatePicker = true
                        },
                        label = stringResource(state.kind.dateLabelRes),
                        icon = painterResource(R.drawable.ic_field_calendar),
                        iconSize = 40.dp,
                        status = state.dateTimeFeedback.statusOrDefault,
                    )
                    IconCardClickableField(
                        valueText = state.time?.format(ReminderTimeFormatter),
                        onClick = {
                            focusManager.clearFocus()
                            showTimePicker = true
                        },
                        label = stringResource(state.kind.timeLabelRes),
                        icon = painterResource(R.drawable.ic_calendar_clock),
                        iconSize = 36.dp,
                        status = state.dateTimeFeedback.statusOrDefault,
                        message = state.dateTimeFeedback?.message?.asString(),
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
                HuellitasButton(
                    text = stringResource(R.string.new_reminder_save),
                    onClick = {
                        focusManager.clearFocus()
                        onAction(NewReminderAction.Save)
                    },
                    enabled = state.isComplete,
                    containerColor = if (state.isComplete) GreenPrimary else ButtonIncomplete,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally).width(279.dp),
                )
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        HuellitasDatePickerDialog(
            initialDate = state.date ?: today,
            minDate = today,
            maxDate = today.plusYears(5),
            confirmText = stringResource(R.string.picker_accept),
            cancelText = stringResource(R.string.picker_cancel),
            onDateSelected = { onAction(NewReminderAction.DateChanged(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        HuellitasTimePickerDialog(
            initialTime = state.time,
            confirmText = stringResource(R.string.picker_accept),
            cancelText = stringResource(R.string.picker_cancel),
            onTimeSelected = { onAction(NewReminderAction.TimeChanged(it)) },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun StepLabel(text: String, modifier: Modifier = Modifier, color: Color = GrayText) {
    Text(text = text, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier.padding(start = 9.dp))
}

/** Tarjeta de una mascota (Figma 3362:1559): verde fuerte si está elegida, verde claro si no. */
@Composable
private fun PetChoiceCard(pet: Pet, isSelected: Boolean, onClick: () -> Unit) {
    val selectedDescription = stringResource(R.string.new_reminder_selected, pet.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(87.dp)
            .clip(CardShape)
            .background(if (isSelected) GreenCard else GreenBackground)
            .semantics { selected = isSelected }
            .clickable(onClickLabel = if (isSelected) selectedDescription else pet.name, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 62.dp, height = 72.dp).clip(CardShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val photoUrl = pet.photoUrl
            if (photoUrl == null) {
                Image(painter = painterResource(R.drawable.ic_pets_breed), contentDescription = null, modifier = Modifier.size(44.dp))
            } else {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = stringResource(R.string.new_reminder_pet_photo, pet.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = pet.name,
            color = Color.Black,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Paso 2 en la caja verde (Figma 3362:1572): el selector de vacuna o motivo, que abre su lista
 * dentro de la misma caja, o el nombre del recordatorio. "Otros" pide además el nombre.
 */
@Composable
private fun StepTwo(state: NewReminderUiState, onAction: (NewReminderAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(CardShape).background(GreenCard).padding(start = 9.dp, end = 9.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val stepRes = when (state.kind) {
            ReminderKind.VACCINE -> R.string.new_reminder_step_vaccine
            ReminderKind.VET -> R.string.new_reminder_step_reason
            ReminderKind.OTHER -> R.string.new_reminder_step_title
        }
        StepLabel(stringResource(stepRes), color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
        when (state.kind) {
            ReminderKind.VACCINE -> OptionPicker(
                isOpen = state.isOptionsOpen,
                options = VaccineType.entries,
                selected = state.vaccineType,
                placeholder = stringResource(R.string.new_reminder_pick_vaccine),
                labelRes = { it.labelRes },
                iconRes = { it.iconRes },
                onToggle = { onAction(NewReminderAction.ToggleOptions) },
                onSelect = { onAction(NewReminderAction.VaccineSelected(it)) },
            )
            ReminderKind.VET -> OptionPicker(
                isOpen = state.isOptionsOpen,
                options = VetReason.entries,
                selected = state.vetReason,
                placeholder = stringResource(R.string.new_reminder_pick_reason),
                labelRes = { it.labelRes },
                iconRes = { it.iconRes },
                onToggle = { onAction(NewReminderAction.ToggleOptions) },
                onSelect = { onAction(NewReminderAction.VetReasonSelected(it)) },
            )
            ReminderKind.OTHER -> Unit
        }
        if (state.needsTitle) {
            IconCardTextField(
                value = state.title,
                onValueChange = { onAction(NewReminderAction.TitleChanged(it)) },
                label = stringResource(R.string.new_reminder_title_label),
                icon = painterResource(R.drawable.ic_calendar_other),
                iconSize = 32.dp,
                status = state.titleFeedback.statusOrDefault,
                message = state.titleFeedback?.message?.asString(),
            )
        }
    }
}

/** Fila blanca con la opción elegida y ">" (Figma 3362:1588); abierta, muestra todas las opciones (3241:1381). */
@Composable
private fun <T> OptionPicker(
    isOpen: Boolean,
    options: List<T>,
    selected: T?,
    placeholder: String,
    labelRes: (T) -> Int,
    iconRes: (T) -> Int,
    onToggle: () -> Unit,
    onSelect: (T) -> Unit,
) {
    if (isOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                OptionRow(
                    label = stringResource(labelRes(option)),
                    iconRes = iconRes(option),
                    showChevron = false,
                    isPlaceholder = false,
                    onClick = { onSelect(option) },
                )
            }
        }
    } else {
        OptionRow(
            label = selected?.let { stringResource(labelRes(it)) } ?: placeholder,
            iconRes = selected?.let(iconRes) ?: R.drawable.ic_calendar_more,
            showChevron = true,
            isPlaceholder = selected == null,
            onClick = onToggle,
        )
    }
}

@Composable
private fun OptionRow(label: String, iconRes: Int, showChevron: Boolean, isPlaceholder: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CardShape)
            .background(Color.White)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(GreenBackground), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.Black),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (isPlaceholder) GrayText else Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        if (showChevron) {
            Image(
                painter = painterResource(R.drawable.ic_calendar_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val FieldFeedback?.statusOrDefault: FieldStatus get() = this?.status ?: FieldStatus.Default

private val PreviewPets = listOf(
    Pet("pet-1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.of(2026, 6, 6), PetSex.MALE, 5.0, null),
    Pet("pet-2", "Maximiliano Cuarto", PetSpecies.DOG, "Jack Russell", LocalDate.of(2026, 3, 1), PetSex.MALE, 6.0, null),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NewReminderVaccinePreview() {
    HuellitasYATheme {
        NewReminderScreen(
            state = NewReminderUiState(
                kind = ReminderKind.VACCINE,
                pets = PreviewPets,
                selectedPetId = "pet-1",
                vaccineType = VaccineType.RABIES,
                date = LocalDate.of(2026, 11, 23),
                time = LocalTime.of(11, 0),
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NewReminderVetOpenPreview() {
    HuellitasYATheme {
        NewReminderScreen(
            state = NewReminderUiState(kind = ReminderKind.VET, pets = PreviewPets, isOptionsOpen = true),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NewReminderOtherPreview() {
    HuellitasYATheme {
        NewReminderScreen(
            state = NewReminderUiState(kind = ReminderKind.OTHER, pets = PreviewPets, selectedPetId = "pet-2", title = "Vacuna contra la rabia"),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}
