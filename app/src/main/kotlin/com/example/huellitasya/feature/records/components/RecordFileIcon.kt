package com.example.huellitasya.feature.records.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.model.MedicalRecord

/**
 * Ícono de un archivo: el ícono PDF de Figma (3193:3360) o, si es una imagen, su miniatura
 * recortada (desviación de Figma, spec 006). Mientras la miniatura carga se ve el fondo de la tarjeta.
 */
@Composable
internal fun RecordFileIcon(record: MedicalRecord, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (record.isPdf) {
            Image(
                painter = painterResource(R.drawable.ic_records_pdf),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = record.fileUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.padding(size / 10).fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}
