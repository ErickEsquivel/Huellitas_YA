package com.example.huellitasya.feature.calendar.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.PetColors
import com.example.huellitasya.core.designsystem.petColors
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import java.time.LocalDateTime

private val CardShape = RoundedCornerShape(20.dp)

/**
 * Tarjeta de un recordatorio (Figma 3241:1112): 367 × 87 dp con el fondo y el borde del color de
 * su mascota, el ícono del tipo, el título, la hora y la fecha. La usan el Calendario y Notificaciones.
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    colors: PetColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = reminder.displayTitle()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 87.dp)
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CardShape)
            .clickable(onClickLabel = stringResource(R.string.calendar_open, title), role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(reminder.kind.iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.Black),
            modifier = Modifier.size(51.dp).padding(6.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reminder.dateTime.displayTime(),
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = reminder.dateTime.toLocalDate().displayDate(),
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/** Punto y nombre de la mascota sobre su grupo de tarjetas (Figma 3241:1125). */
@Composable
internal fun PetGroupTitle(name: String, colors: PetColors, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(25.dp).clip(CircleShape).background(colors.accent))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            color = colors.accent,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun ReminderCardPreview() {
    HuellitasYATheme {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PetGroupTitle(name = "Maximiliano Tercero", colors = petColors(0))
            ReminderCard(
                reminder = Reminder("1", "pet-1", ReminderKind.VACCINE, VaccineType.RABIES, null, null, LocalDateTime.of(2026, 9, 23, 11, 0)),
                colors = petColors(0),
                onClick = {},
            )
        }
    }
}
