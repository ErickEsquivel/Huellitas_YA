package com.example.huellitasya.feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenCard
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.PinkSurface
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.ui.breedAndAge
import java.time.LocalDate

private val CardShape = RoundedCornerShape(20.dp)

/**
 * Tarjeta verde de una mascota en Home (Figma 9:259): foto en el recuadro rosa, nombre,
 * "raza  -  edad" en blanco y ">" a la derecha. Toda la tarjeta abre el perfil.
 */
@Composable
internal fun PetCard(
    pet: Pet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(87.dp)
            .clip(CardShape)
            .background(GreenCard)
            .clickable(
                onClickLabel = stringResource(R.string.home_open_pet, pet.name),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 10.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetThumbnail(photoUrl = pet.photoUrl, contentDescription = stringResource(R.string.home_pet_photo, pet.name))
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pet.name,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pet.breedAndAge(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // El vector es un "^": a 90° es el "<" de los formularios y a −90° el ">" de Figma.
        Image(
            painter = painterResource(R.drawable.ic_back_chevron),
            contentDescription = null,
            modifier = Modifier.rotate(-90f),
        )
    }
}

/** Recuadro rosa de 62 × 72 dp con la foto recortada; sin foto, el ícono de mascota. */
@Composable
private fun PetThumbnail(photoUrl: String?, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 72.dp)
            .clip(CardShape)
            .background(PinkSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl == null) {
            Image(
                painter = painterResource(R.drawable.ic_pets_breed),
                contentDescription = contentDescription,
                modifier = Modifier.size(46.dp),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PetCardPreview() {
    HuellitasYATheme {
        PetCard(
            pet = Pet(
                id = "pet-1",
                name = "Maximiliano Tercero",
                species = PetSpecies.DOG,
                breed = "Golden Retriever",
                birthDate = LocalDate.now().minusMonths(3),
                sex = PetSex.MALE,
                weightKg = 5.0,
                photoUrl = null,
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
