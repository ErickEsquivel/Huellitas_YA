package com.example.huellitasya.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LightGrayText
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.ScreenHeader
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.UserProfile
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.profile.components.BirthDateFormatter
import com.example.huellitasya.feature.profile.components.ProfileAvatar
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun ProfileRoute(
    onEditClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // `null` = el usuario cerró el selector sin elegir: no pasa nada.
        uri?.let { viewModel.onAction(ProfileAction.PhotoPicked(it.toString())) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ProfileEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    ProfileScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onEditClick = onEditClick,
        onPhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

/** Figma "PERFIL" (99:723). */
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onEditClick: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    val profile = state.profile
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Figma (99:723): la ola empieza poco antes del botón, sin tapar los datos.
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.2f)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // En pantallas bajas los datos se desplazan; el botón queda siempre visible.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Figma pone el título a 45 dp incluyendo la barra de estado, que el Scaffold ya descuenta.
                Spacer(modifier = Modifier.height(24.dp))

                ScreenHeader(
                    title = stringResource(R.string.profile_title),
                    subtitle = stringResource(R.string.profile_subtitle),
                    notificationsDescription = stringResource(R.string.profile_notifications),
                )

                Spacer(modifier = Modifier.height(24.dp))

                ProfileAvatar(
                    photoUrl = profile?.photoUrl,
                    contentDescription = stringResource(R.string.profile_photo_description),
                    changePhotoDescription = stringResource(R.string.profile_change_photo),
                    isUploading = state.isUploadingPhoto,
                    onClick = onPhotoClick,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profile?.name.orEmpty(),
                    color = Color.Black,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProfileInfoItem(
                        label = stringResource(R.string.profile_field_email),
                        value = profile?.email,
                        isLink = true,
                    )
                    ProfileInfoItem(label = stringResource(R.string.profile_field_phone), value = profile?.phone)
                    ProfileInfoItem(label = stringResource(R.string.profile_field_address), value = profile?.address)
                    ProfileInfoItem(
                        label = stringResource(R.string.profile_field_birth_date),
                        value = profile?.birthDate?.format(BirthDateFormatter),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            HuellitasButton(
                text = stringResource(R.string.profile_edit),
                onClick = onEditClick,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Etiqueta gris + valor. Los datos opcionales vacíos se muestran como "Sin registrar". */
@Composable
private fun ProfileInfoItem(label: String, value: String?, isLink: Boolean = false) {
    Column {
        Text(
            text = label,
            color = GrayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value ?: stringResource(R.string.profile_not_registered),
            color = if (value == null) LightGrayText else Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = if (isLink && value != null) TextDecoration.Underline else TextDecoration.None,
        )
    }
}

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

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ProfileScreenPreview() {
    HuellitasYATheme {
        ProfileScreen(
            state = ProfileUiState(profile = PreviewProfile),
            snackbarHostState = SnackbarHostState(),
            onEditClick = {},
            onPhotoClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ProfileScreenEmptyFieldsPreview() {
    HuellitasYATheme {
        ProfileScreen(
            state = ProfileUiState(profile = PreviewProfile.copy(phone = null, address = null, birthDate = null)),
            snackbarHostState = SnackbarHostState(),
            onEditClick = {},
            onPhotoClick = {},
        )
    }
}
