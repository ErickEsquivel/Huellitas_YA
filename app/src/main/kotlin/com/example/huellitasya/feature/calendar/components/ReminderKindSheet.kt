package com.example.huellitasya.feature.calendar.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.model.ReminderKind

/** "¿Qué deseas programar?…" (Figma 3241:1717): Vacunas, Veterinario u Otro. Se cierra tocando fuera. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderKindSheet(onKindSelected: (ReminderKind) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GreenBackground,
    ) {
        ReminderKindSheetContent(onKindSelected = onKindSelected)
    }
}

private val OptionShape = RoundedCornerShape(20.dp)

@Composable
private fun ReminderKindSheetContent(onKindSelected: (ReminderKind) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_sheet_title),
            color = Color.Black,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp),
        )
        ReminderKind.entries.forEach { kind ->
            val label = stringResource(kind.labelRes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(OptionShape)
                    .background(Color.White)
                    .border(1.dp, BorderSubtle, OptionShape)
                    .clickable(onClickLabel = label, role = Role.Button) { onKindSelected(kind) }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(kind.iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(text = label, color = Color.Black, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, backgroundColor = 0xFFC7EBC7)
@Composable
private fun ReminderKindSheetPreview() {
    HuellitasYATheme { ReminderKindSheetContent(onKindSelected = {}) }
}
