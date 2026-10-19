package com.example.huellitasya.feature.pets.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.ProgressOverlay
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.pets.components.PetFormFields
import com.example.huellitasya.feature.pets.components.PetPhotoPicker
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
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // Huellas decorativas de Figma (404 × 446 dp al 30 %).
            Image(
                painter = painterResource(R.drawable.img_pets_paws),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(446.dp)
                    .alpha(0.3f),
            )
            CurvedBackground(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.64f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Edge-to-edge: el botón no debe quedar debajo de la barra de navegación ni del teclado.
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                HuellitasBackButton(
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.pets_register_back),
                    modifier = Modifier.align(Alignment.Start).offset(x = 6.dp),
                )
                Text(
                    text = stringResource(R.string.pets_register_title),
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                PetPhotoPicker(
                    photoModel = state.form.photoModel,
                    contentDescription = stringResource(R.string.pets_photo_description),
                    changePhotoDescription = stringResource(R.string.pets_photo_change),
                    onClick = onPhotoClick,
                    modifier = Modifier.offset(x = 10.dp),
                )
                Spacer(modifier = Modifier.height(44.dp))
                PetFormFields(
                    form = state.form,
                    onAction = { onAction(RegisterPetAction.Form(it)) },
                )
                Spacer(modifier = Modifier.height(48.dp))
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
                Spacer(modifier = Modifier.height(32.dp))
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
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
