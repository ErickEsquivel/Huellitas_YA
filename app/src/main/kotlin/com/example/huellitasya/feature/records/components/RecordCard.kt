package com.example.huellitasya.feature.records.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.RecordCardSurface
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import java.time.LocalDate

private val CardShape = RoundedCornerShape(10.dp)

/**
 * Tarjeta de un archivo en la lista (Figma 3193:3350): ícono, nombre y "Fecha | Tipo" con el
 * veterinario debajo si lo hay. Toda la tarjeta abre el detalle.
 */
@Composable
internal fun RecordCard(
    record: MedicalRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .clip(CardShape)
            .background(RecordCardSurface)
            .border(1.dp, BorderSubtle, CardShape)
            .clickable(
                onClickLabel = stringResource(R.string.records_open, record.fileName),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordFileIcon(record = record, size = 70.dp, modifier = Modifier.size(70.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.fileName,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.records_card_date_type,
                    record.date.format(RecordDateFormatter),
                    stringResource(record.type.labelRes),
                ),
                color = Color.Black,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
                fontWeight = FontWeight.Bold,
            )
            record.vet?.let { vet ->
                Text(
                    text = stringResource(R.string.records_card_vet, vet),
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordCardPreview() {
    HuellitasYATheme {
        RecordCard(
            record = MedicalRecord(
                id = "1",
                petId = "pet-1",
                category = RecordCategory.VACCINES,
                fileName = "Vacuna_Rabia_2026.pdf",
                contentType = "application/pdf",
                sizeBytes = 120_000,
                date = LocalDate.of(2026, 9, 10),
                type = RecordType.VACCINATION,
                vet = "Dr. A. Ramírez",
                fileUrl = "",
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
