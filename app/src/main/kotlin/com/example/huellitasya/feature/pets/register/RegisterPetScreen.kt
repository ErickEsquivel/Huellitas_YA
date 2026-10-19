package com.example.huellitasya.feature.pets.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.ProgressOverlay
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.pets.components.PetFormLayout
import com.example.huellitasya.feature.pets.form.PetFormAction
import com.example.huellitasya.feature.pets.form.PetFormState
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun RegisterPetRoute(
    onBackClick: () -> Unit,
    onPetSaved: () -> Unit,
    viewModel: RegisterPetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // `null` = el usuario cerró el selector sin elegir: no pasa nada.
        uri?.let { viewModel.onAction(RegisterPetAction.Form(PetFormAction.PhotoPicked(it.toString()))) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RegisterPetEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            RegisterPetEvent.PetSaved -> onPetSaved()
        }
    }

    RegisterPetScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onPhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

/** Figma "REGISTRO MASCOTAS" (158:1359); campos vacíos 158:1230; lleno 19:63; guardando 3234:1022. */
@Composable
internal fun RegisterPetScreen(
    state: RegisterPetUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (RegisterPetAction) -> Unit,
    onBackClick: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    ProgressOverlay(isVisible = state.isSaving, text = stringResource(R.string.pets_register_saving)) {
        PetFormLayout(
            title = stringResource(R.string.pets_register_title),
            form = state.form,
            snackbarHostState = snackbarHostState,
            onFormAction = { onAction(RegisterPetAction.Form(it)) },
            onBackClick = onBackClick,
            onPhotoClick = onPhotoClick,
        ) {
            HuellitasButton(
                text = stringResource(R.string.pets_register_button),
                onClick = {
                    focusManager.clearFocus()
                    onAction(RegisterPetAction.Save)
                },
                containerColor = if (state.form.isComplete) GreenPrimary else ButtonIncomplete,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RegisterPetScreenEmptyPreview() {
    HuellitasYATheme {
        RegisterPetScreen(
            state = RegisterPetUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onPhotoClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RegisterPetScreenFilledPreview() {
    HuellitasYATheme {
        RegisterPetScreen(
            state = RegisterPetUiState(
                form = PetFormState(
                    name = "Maximiliano Tercero",
                    species = PetSpecies.DOG,
                    breed = "Golden Retriever",
                    birthDate = LocalDate.of(2026, 6, 6),
                    sex = PetSex.MALE,
                    weight = "5",
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onPhotoClick = {},
        )
    }
}
