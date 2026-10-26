package com.example.huellitasya.feature.pets.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.greenWave
import com.example.huellitasya.feature.pets.form.PetFormAction
import com.example.huellitasya.feature.pets.form.PetFormState

/**
 * Esqueleto común de Registrar (Figma 19:63) y Editar mascota (56:192): huellas decorativas, "<",
 * título, foto, la ola verde con los campos y, debajo, los botones de cada pantalla ([buttons]).
 */
@Composable
internal fun PetFormLayout(
    title: String,
    form: PetFormState,
    snackbarHostState: SnackbarHostState,
    onFormAction: (PetFormAction) -> Unit,
    onBackClick: () -> Unit,
    onPhotoClick: () -> Unit,
    titleFontSize: TextUnit = 24.sp,
    buttons: @Composable ColumnScope.() -> Unit,
) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Edge-to-edge: los botones no deben quedar debajo de la barra de navegación ni del teclado.
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
                text = title,
                color = Color.Black,
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            PetPhotoPicker(
                photoModel = form.photoModel,
                contentDescription = stringResource(R.string.pets_photo_description),
                changePhotoDescription = stringResource(R.string.pets_photo_change),
                onClick = onPhotoClick,
                modifier = Modifier.offset(x = 10.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .greenWave(horizontalBleed = 24.dp)
                    .padding(top = 64.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PetFormFields(form = form, onAction = onFormAction)
                Spacer(modifier = Modifier.height(48.dp))
                buttons()
            }
        }

        // Sin barra inferior, el aviso debe quedar encima de la barra de navegación.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}
