package com.example.huellitasya.feature.profile.personaldata

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.PasswordConfirmDialog
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.UserProfile
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.profile.components.BirthDateFormatter
import com.example.huellitasya.feature.profile.components.PersonalDataField
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun PersonalDataRoute(
    onBackClick: () -> Unit,
    viewModel: PersonalDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PersonalDataEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    PersonalDataScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}

/** Figma "CONFIGURACION" → Datos Personales (3254:1023). */
@Composable
internal fun PersonalDataScreen(
    state: PersonalDataUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onAction: (PersonalDataAction) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.58f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            HuellitasBackButton(
                onClick = onBackClick,
                contentDescription = stringResource(R.string.personal_data_back),
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.personal_data_title),
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(56.dp))

            OwnerDataCard(
                state = state,
                onAction = onAction,
                onBirthDateClick = { showDatePicker = true },
            )

            Spacer(modifier = Modifier.height(48.dp))
            HuellitasButton(
                text = stringResource(R.string.personal_data_save),
                onClick = { onAction(PersonalDataAction.Save) },
                containerColor = if (state.hasChanges) GreenAction else ButtonIncomplete,
                isLoading = state.isSaving,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    state.reauth?.let { reauth ->
        PasswordConfirmDialog(
            title = stringResource(R.string.personal_data_reauth_title),
            message = stringResource(R.string.personal_data_reauth_message),
            password = reauth.password,
            onPasswordChange = { onAction(PersonalDataAction.ReauthPasswordChanged(it)) },
            passwordLabel = stringResource(R.string.personal_data_reauth_password),
            showPasswordDescription = stringResource(R.string.personal_data_reauth_show_password),
            hidePasswordDescription = stringResource(R.string.personal_data_reauth_hide_password),
            confirmText = stringResource(R.string.personal_data_reauth_confirm),
            dismissText = stringResource(R.string.personal_data_reauth_cancel),
            onConfirm = { onAction(PersonalDataAction.ConfirmReauth) },
            onDismiss = { onAction(PersonalDataAction.DismissReauth) },
            status = reauth.feedback.statusOrDefault,
            statusMessage = reauth.feedback?.message?.asString(),
            isLoading = reauth.isLoading,
        )
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initialDate = state.birthDate,
            onDateSelected = { onAction(PersonalDataAction.BirthDateChanged(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** Tarjeta "Datos Dueño": título, lápiz y los cinco campos subrayados. */
@Composable
private fun OwnerDataCard(
    state: PersonalDataUiState,
    onAction: (PersonalDataAction) -> Unit,
    onBirthDateClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(10.dp)
    val notRegistered = stringResource(R.string.profile_not_registered).takeUnless { state.isEditing }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, cardShape)
            .background(Color.White, cardShape)
            .padding(start = 28.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.personal_data_card_title),
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAction(PersonalDataAction.ToggleEditing) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile_edit),
                    contentDescription = stringResource(
                        if (state.isEditing) R.string.personal_data_cancel_edit else R.string.personal_data_edit,
                    ),
                    tint = if (state.isEditing) GreenPrimary else Color.Black,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PersonalDataField(
                label = stringResource(R.string.profile_field_name),
                value = state.name,
                onValueChange = { onAction(PersonalDataAction.NameChanged(it)) },
                editable = state.isEditing,
                status = state.nameFeedback.statusOrDefault,
                message = state.nameFeedback?.message?.asString(),
            )
            PersonalDataField(
                label = stringResource(R.string.profile_field_email),
                value = state.email,
                onValueChange = { onAction(PersonalDataAction.EmailChanged(it)) },
                // En cuentas Google el correo pertenece a Google: solo lectura (spec 004).
                editable = state.isEditing && state.canEditEmail,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                status = state.emailFeedback.statusOrDefault,
                message = state.emailFeedback?.message?.asString(),
            )
            PersonalDataField(
                label = stringResource(R.string.profile_field_phone),
                value = state.phone,
                onValueChange = { onAction(PersonalDataAction.PhoneChanged(it)) },
                editable = state.isEditing,
                placeholder = notRegistered,
                status = state.phoneFeedback.statusOrDefault,
                message = state.phoneFeedback?.message?.asString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
            PersonalDataField(
                label = stringResource(R.string.profile_field_address),
                value = state.address,
                onValueChange = { onAction(PersonalDataAction.AddressChanged(it)) },
                editable = state.isEditing,
                placeholder = notRegistered,
                status = state.addressFeedback.statusOrDefault,
                message = state.addressFeedback?.message?.asString(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            PersonalDataField(
                label = stringResource(R.string.profile_field_birth_date),
                value = state.birthDate?.format(BirthDateFormatter).orEmpty(),
                onValueChange = {},
                editable = state.isEditing,
                placeholder = notRegistered,
                status = state.birthDateFeedback.statusOrDefault,
                message = state.birthDateFeedback?.message?.asString(),
                onClick = onBirthDateClick,
            )
        }

        state.formFeedback?.message?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            FieldMessage(text = message.asString(), status = state.formFeedback.statusOrDefault)
        }
    }
}

private val FieldFeedback?.statusOrDefault: FieldStatus get() = this?.status ?: FieldStatus.Default

/**
 * Selector de Material 3. Trabaja en UTC (así lo define `DatePicker`) para que la fecha elegida
 * no se corra un día por la zona horaria. "Borrar" deja la fecha sin registrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = MinBirthYear..LocalDate.now().year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !utcTimeMillis.toLocalDate().isAfter(LocalDate.now())

            override fun isSelectableYear(year: Int): Boolean = year in MinBirthYear..LocalDate.now().year
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
            ) { Text(stringResource(R.string.personal_data_date_accept)) }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onDateSelected(null)
                        onDismiss()
                    },
                ) { Text(stringResource(R.string.personal_data_date_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.personal_data_date_cancel)) }
            }
        },
    ) {
        DatePicker(state = datePickerState, colors = colors)
    }
}

private const val MinBirthYear = 1900

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val PreviewProfile = UserProfile(
    uid = "uid-123",
    name = "Santiago Cruz",
    email = "sa.company@hotmail.com",
    phone = "+573124456311",
    address = "61a Sur27 Cra. 104a Bis",
    birthDate = LocalDate.of(2005, 1, 15),
    photoUrl = null,
    provider = AuthProvider.EMAIL,
)

private val PreviewState = PersonalDataUiState(
    saved = PreviewProfile,
    name = PreviewProfile.name,
    email = PreviewProfile.email,
    phone = PreviewProfile.phone.orEmpty(),
    address = PreviewProfile.address.orEmpty(),
    birthDate = PreviewProfile.birthDate,
)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PersonalDataScreenPreview() {
    HuellitasYATheme {
        PersonalDataScreen(
            state = PreviewState,
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PersonalDataScreenEditingPreview() {
    HuellitasYATheme {
        PersonalDataScreen(
            state = PreviewState.copy(
                isEditing = true,
                phone = "312",
                phoneFeedback = FieldFeedback(FieldStatus.Warning),
                hasChanges = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}
