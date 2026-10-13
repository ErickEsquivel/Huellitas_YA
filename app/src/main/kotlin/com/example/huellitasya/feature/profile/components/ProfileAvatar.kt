package com.example.huellitasya.feature.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenBackground
import java.time.format.DateTimeFormatter

/** Formato de la fecha de nacimiento en Perfil y Datos Personales (Figma: `15/01/2005`). */
internal val BirthDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Foto circular sobre el círculo verde de Figma (99:723, 150 dp). Sin foto, o si no carga,
 * se ve `img_profile_placeholder`.
 */
@Composable
internal fun ProfileAvatar(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
) {
    val placeholder = painterResource(R.drawable.img_profile_placeholder)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GreenBackground),
    ) {
        if (photoUrl == null) {
            Image(
                painter = placeholder,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}
