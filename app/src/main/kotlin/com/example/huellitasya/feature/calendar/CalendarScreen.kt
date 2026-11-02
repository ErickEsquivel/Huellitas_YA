package com.example.huellitasya.feature.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenAccent
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LightGrayText
import com.example.huellitasya.core.designsystem.SurfaceGray
import com.example.huellitasya.core.designsystem.components.AddPlaceholderCard
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.ScreenHeader
import com.example.huellitasya.core.designsystem.petColors
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.calendar.components.PetGroupTitle
import com.example.huellitasya.feature.calendar.components.ReminderCard
import com.example.huellitasya.feature.calendar.components.displayMonth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Composable
internal fun CalendarRoute(
    onAddClick: () -> Unit,
    onReminderClick: (reminderId: String) -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CalendarEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    CalendarScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onAddClick = onAddClick,
        onReminderClick = onReminderClick,
        onNotificationsClick = onNotificationsClick,
    )
}

/**
 * Calendario (Figma: vacío 3444:828, con eventos 3241:1074): el mes con hoy y los días con
 * eventos marcados, el "+" para programar y "Próximos eventos" agrupados por mascota.
 */
@Composable
internal fun CalendarScreen(
    state: CalendarUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (CalendarAction) -> Unit,
    onAddClick: () -> Unit,
    onReminderClick: (reminderId: String) -> Unit,
    onNotificationsClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.6f))

        // Solo "Próximos eventos" se desplaza (Figma 3336:1114); la cabecera y el mes quedan fijos.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 21.dp, end = 21.dp, top = 45.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.calendar_title),
                subtitle = stringResource(R.string.calendar_subtitle),
                notificationsDescription = stringResource(R.string.calendar_notifications),
                onNotificationsClick = onNotificationsClick,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                MonthCard(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.padding(bottom = 25.dp),
                )
                AddReminderButton(onClick = onAddClick, modifier = Modifier.align(Alignment.BottomCenter))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.calendar_upcoming),
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            ) {
                if (!state.isLoading) {
                    if (state.upcoming.isEmpty()) {
                        AddPlaceholderCard(
                            contentDescription = stringResource(R.string.calendar_add),
                            onClick = onAddClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            state.upcoming.forEach { group ->
                                val colors = petColors(group.colorIndex)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PetGroupTitle(name = group.pet.name, colors = colors)
                                    group.reminders.forEach { reminder ->
                                        ReminderCard(reminder = reminder, colors = colors, onClick = { onReminderClick(reminder.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private val MonthCardShape = RoundedCornerShape(20.dp)

/** Tarjeta del mes (Figma 3241:1107): 332 dp de ancho, borde verde, "<" mes ">" y la cuadrícula de lunes a domingo. */
@Composable
private fun MonthCard(state: CalendarUiState, onAction: (CalendarAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 332.dp)
            .fillMaxWidth()
            .clip(MonthCardShape)
            .background(Color.White)
            .border(1.dp, GreenPrimary, MonthCardShape)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 30.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(
                iconRes = R.drawable.ic_calendar_chevron_left,
                description = stringResource(R.string.calendar_previous_month),
                onClick = { onAction(CalendarAction.PreviousMonth) },
            )
            Text(
                text = state.month.displayMonth(),
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            MonthArrow(
                iconRes = R.drawable.ic_calendar_chevron_right,
                description = stringResource(R.string.calendar_next_month),
                onClick = { onAction(CalendarAction.NextMonth) },
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            stringArrayResource(R.array.calendar_weekdays).forEach { weekday ->
                Text(
                    text = weekday,
                    color = GrayText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        monthWeeks(state.month).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                isToday = state.month.atDay(day) == state.today,
                                markColorIndex = state.dayMarks[day],
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(iconRes: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painter = painterResource(iconRes), contentDescription = description, tint = Color.Black, modifier = Modifier.size(16.dp))
    }
}

/** Número del día: hoy sobre un círculo gris; con eventos, dentro de un anillo del color de la mascota (Figma 3241:1108-1111). */
@Composable
private fun DayCell(day: Int, isToday: Boolean, markColorIndex: Int?) {
    val today = stringResource(R.string.calendar_today)
    val withEvents = stringResource(R.string.calendar_day_with_events, day)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(if (isToday) Modifier.background(SurfaceGray).border(1.dp, LightGrayText, CircleShape) else Modifier)
            .then(if (markColorIndex != null) Modifier.border(1.dp, petColors(markColorIndex).accent, CircleShape) else Modifier)
            .semantics {
                if (markColorIndex != null) contentDescription = withEvents
                else if (isToday) contentDescription = "$day, $today"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            color = if (isToday) GrayText else Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** El "+" verde sobre el borde inferior del mes (Figma 3241:1140): 50 dp. */
@Composable
private fun AddReminderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(GreenAccent)
            .clickable(onClickLabel = stringResource(R.string.calendar_add), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_records_add),
            contentDescription = stringResource(R.string.calendar_add),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Semanas del mes de lunes a domingo; `null` en los huecos antes del 1 y después del último día. */
internal fun monthWeeks(month: YearMonth): List<List<Int?>> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).toList()
    return cells.chunked(7).map { week -> week + List(7 - week.size) { null } }
}

private val PreviewPets = listOf(
    Pet("pet-1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.of(2026, 6, 6), PetSex.MALE, 5.0, null),
    Pet("pet-2", "Maximiliano Cuarto", PetSpecies.DOG, "Jack Russell", LocalDate.of(2026, 3, 1), PetSex.MALE, 6.0, null),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun CalendarScreenPreview() {
    HuellitasYATheme {
        CalendarScreen(
            state = CalendarUiState(
                month = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 18),
                dayMarks = mapOf(23 to 0, 24 to 1, 28 to 2),
                upcoming = listOf(
                    UpcomingGroup(PreviewPets[0], 0, listOf(Reminder("1", "pet-1", ReminderKind.VACCINE, VaccineType.RABIES, null, null, LocalDateTime.of(2026, 9, 23, 11, 0)))),
                    UpcomingGroup(PreviewPets[1], 1, listOf(Reminder("2", "pet-2", ReminderKind.VACCINE, VaccineType.RABIES, null, null, LocalDateTime.of(2026, 9, 24, 9, 0)))),
                ),
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onAddClick = {},
            onReminderClick = {},
            onNotificationsClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun CalendarScreenEmptyPreview() {
    HuellitasYATheme {
        CalendarScreen(
            state = CalendarUiState(month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 18), isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onAddClick = {},
            onReminderClick = {},
            onNotificationsClick = {},
        )
    }
}
