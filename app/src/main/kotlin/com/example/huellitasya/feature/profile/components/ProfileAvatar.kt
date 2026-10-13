package com.example.huellitasya.feature.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.OverlayScrim
import java.time.format.DateTimeFormatter

/** Formato de la fecha de nacimiento en Perfil y Datos Personales (Figma: `15/01/2005`). */
internal val BirthDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Foto circular sobre el círculo verde de Figma (99:723, 150 dp). Sin foto, o si no carga, se ve
 * `img_profile_placeholder`. La insignia de cámara indica que se puede tocar para cambiarla
 * (spec 004, "Desviaciones de Figma").
 */
@Composable
internal fun ProfileAvatar(
    photoUrl: String?,
    contentDescription: String,
    changePhotoDescription: String,
    isUploading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clickable(
                enabled = !isUploading,
                onClickLabel = changePhotoDescription,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        AvatarImage(photoUrl = photoUrl, contentDescription = contentDescription, size = size)

        if (isUploading) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(OverlayScrim),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(GreenPrimary)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_profile_camera),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AvatarImage(photoUrl: String?, contentDescription: String, size: Dp) {
    val placeholder = painterResource(R.drawable.img_profile_placeholder)
    val imageModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(GreenBackground)
    if (photoUrl == null) {
        Image(
            painter = placeholder,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = contentDescription,
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileAvatarPreview() {
    HuellitasYATheme {
        ProfileAvatar(
            photoUrl = null,
            contentDescription = "Foto de perfil",
            changePhotoDescription = "Cambiar foto de perfil",
            isUploading = false,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileAvatarUploadingPreview() {
    HuellitasYATheme {
        ProfileAvatar(
            photoUrl = null,
            contentDescription = "Foto de perfil",
            changePhotoDescription = "Cambiar foto de perfil",
            isUploading = true,
            onClick = {},
        )
    }
}
