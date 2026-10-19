package com.example.huellitasya.feature.pets.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Diámetro del círculo verde de Figma (19:63: 222 × 214 dp; se usa un círculo). */
private val PhotoSize = 214.dp

/**
 * Foto de la mascota en el formulario (Figma 158:1359 y 19:63). Sin foto se ve el placeholder
 * desenfocado con el ícono de subir imagen; con foto, se recorta en el círculo verde. La cámara
 * de Figma va abajo a la derecha. Tocar cualquier parte abre el selector.
 *
 * @param photoModel `content://` recién elegido o URL de Storage; `null` si no hay foto.
 */
@Composable
internal fun PetPhotoPicker(
    photoModel: String?,
    contentDescription: String,
    changePhotoDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(PhotoSize + 20.dp)
            .clickable(onClickLabel = changePhotoDescription, role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(PhotoSize)
                .clip(CircleShape)
                // Figma: sin foto, el círculo es solo el placeholder al 20 %; con foto, va sobre el verde.
                .background(if (photoModel == null) Color.White else GreenBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (photoModel == null) {
                Image(
                    painter = painterResource(R.drawable.img_pets_photo_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.2f),
                )
                Image(
                    painter = painterResource(R.drawable.ic_pets_upload_photo),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(74.dp),
                )
            } else {
                AsyncImage(
                    model = photoModel,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.img_pets_camera),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 171.dp, y = 161.dp)
                .size(width = 58.dp, height = 53.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PetPhotoPickerPreview() {
    HuellitasYATheme {
        PetPhotoPicker(
            photoModel = null,
            contentDescription = "Foto de la mascota",
            changePhotoDescription = "Elegir foto",
            onClick = {},
        )
    }
}
