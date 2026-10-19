package com.example.huellitasya.feature.pets.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
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
import com.example.huellitasya.core.designsystem.DestructiveRed
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.DeleteConfirmDialog
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
internal fun EditPetRoute(
    onBackClick: () -> Unit,
    onPetUpdated: () -> Unit,
    onPetDeleted: () -> Unit,
    viewModel: EditPetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // `null` = el usuario cerró el selector sin elegir: no pasa nada.
        uri?.let { viewModel.onAction(EditPetAction.Form(PetFormAction.PhotoPicked(it.toString()))) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EditPetEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            EditPetEvent.Updated -> onPetUpdated()
            EditPetEvent.Deleted -> onPetDeleted()
        }
    }

    EditPetScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onPhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

/**
 * Figma "EDIT PERRO 1" (56:192); actualizando 3242:2283; diálogo 3050:1515; eliminando 3242:2009.
 * Mismo formulario que Registrar ([PetFormLayout]) con "Actualizar información" y "Eliminar mascota".
 */
@Composable
internal fun EditPetScreen(
    state: EditPetUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (EditPetAction) -> Unit,
    onBackClick: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val overlayText = stringResource(if (state.isDeleting) R.string.pets_edit_deleting else R.string.pets_edit_updating)

    ProgressOverlay(isVisible = state.isBusy, text = overlayText) {
        PetFormLayout(
            title = stringResource(R.string.pets_edit_title),
            titleFontSize = 22.sp,
            form = state.form,
            snackbarHostState = snackbarHostState,
            onFormAction = { onAction(EditPetAction.Form(it)) },
            onBackClick = onBackClick,
            onPhotoClick = onPhotoClick,
        ) {
            HuellitasButton(
                text = stringResource(R.string.pets_edit_button),
                onClick = {
                    focusManager.clearFocus()
                    onAction(EditPetAction.Update)
                },
                containerColor = if (state.hasChanges) GreenPrimary else ButtonIncomplete,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HuellitasButton(
                text = stringResource(R.string.pets_edit_delete),
                onClick = {
                    focusManager.clearFocus()
                    onAction(EditPetAction.DeleteClick)
                },
                containerColor = DestructiveRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )
        }
    }

    if (state.showDeleteDialog) {
        DeleteConfirmDialog(
            title = stringResource(R.string.pets_delete_title),
            message = stringResource(R.string.pets_delete_message),
            confirmText = stringResource(R.string.pets_delete_confirm),
            closeDescription = stringResource(R.string.pets_delete_close),
            imageDescription = stringResource(R.string.pets_delete_image_description),
            onConfirm = { onAction(EditPetAction.ConfirmDelete) },
            onDismiss = { onAction(EditPetAction.DismissDelete) },
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun EditPetScreenPreview() {
    HuellitasYATheme {
        EditPetScreen(
            state = EditPetUiState(
                form = PetFormState(
                    name = "Maximiliano Tercero",
                    species = PetSpecies.DOG,
                    breed = "Golden Retriever",
                    birthDate = LocalDate.of(2026, 6, 6),
                    sex = PetSex.MALE,
                    weight = "5",
                ),
                hasChanges = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onPhotoClick = {},
        )
    }
}
