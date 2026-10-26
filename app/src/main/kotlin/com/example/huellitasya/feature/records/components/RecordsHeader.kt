package com.example.huellitasya.feature.records.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.greenWave
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.ui.breedAndAge

/**
 * Cabecera común de los registros médicos (Figma 3193:3339): "<", "Perfil de <nombre>", la foto con
 * "raza  -  edad" a su derecha, "Registros Médicos - Registros Detallados" y la categoría. La ola
 * verde empieza detrás de la foto y sigue hasta el borde inferior de la pantalla.
 *
 * Va dentro de una columna con 24 dp de padding horizontal. Sin [category] (aún cargando), la
 * línea de la categoría queda vacía.
 */
@Composable
internal fun RecordsHeader(
    pet: Pet?,
    category: RecordCategory?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            HuellitasBackButton(
                onClick = onBackClick,
                contentDescription = stringResource(R.string.records_back),
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp),
            )
            Text(
                text = pet?.let { stringResource(R.string.records_pet_title, it.name) }.orEmpty(),
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            // La ola empieza a media altura de la foto, como en Figma.
            Box(modifier = Modifier.matchParentSize().padding(top = 96.dp).greenWave(horizontalBleed = 24.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PetHeaderPhoto(pet = pet)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = pet?.breedAndAge().orEmpty(),
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
                Spacer(modifier = Modifier.height(56.dp))
                // En pantallas de menos de 412 dp el título se reduce para caber en una línea, como en Figma.
                Text(
                    text = stringResource(R.string.records_section_title),
                    color = Color.Black,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 18.sp),
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = category?.let { stringResource(it.labelRes) }.orEmpty(),
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Foto de 120 dp sobre el círculo verde; sin foto, el ícono de mascota (igual que el perfil, spec 005). */
@Composable
private fun PetHeaderPhoto(pet: Pet?) {
    val description = pet?.let { stringResource(R.string.records_pet_photo, it.name) }
    Box(
        modifier = Modifier.padding(start = 8.dp).size(120.dp).clip(CircleShape).background(GreenBackground),
        contentAlignment = Alignment.Center,
    ) {
        val photoUrl = pet?.photoUrl
        if (photoUrl == null) {
            Image(
                painter = painterResource(R.drawable.ic_pets_breed),
                contentDescription = description,
                modifier = Modifier.size(80.dp),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
