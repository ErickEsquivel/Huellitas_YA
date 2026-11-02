package com.example.huellitasya.feature.calendar.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.petColors
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.model.VetReason
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.calendar.components.ReminderCard
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
internal fun NotificationsRoute(
    onBackClick: () -> Unit,
    onReminderClick: (reminderId: String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NotificationsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    NotificationsScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onReminderClick = onReminderClick,
    )
}

/** Notificaciones (Figma 3263:914): los próximos recordatorios en el color de su mascota. */
@Composable
internal fun NotificationsScreen(
    state: NotificationsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onReminderClick: (reminderId: String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.25f))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                HuellitasBackButton(
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.reminder_detail_back),
                    modifier = Modifier.align(Alignment.TopStart),
                )
                Text(
                    text = stringResource(R.string.notifications_title),
                    color = Color.Black,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
            }
            Text(
                text = stringResource(R.string.notifications_subtitle),
                color = GrayText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Column(modifier = Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                if (!state.isLoading && state.items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notifications_empty),
                        color = GrayText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.items.forEach { item ->
                    ReminderCard(
                        reminder = item.reminder,
                        colors = petColors(item.colorIndex),
                        onClick = { onReminderClick(item.reminder.id) },
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NotificationsScreenPreview() {
    HuellitasYATheme {
        NotificationsScreen(
            state = NotificationsUiState(
                items = listOf(
                    NotificationItem(Reminder("1", "pet-1", ReminderKind.VACCINE, VaccineType.RABIES, null, null, LocalDateTime.of(2026, 11, 23, 11, 0)), 0),
                    NotificationItem(Reminder("2", "pet-2", ReminderKind.VACCINE, VaccineType.RABIES, null, null, LocalDateTime.of(2026, 11, 24, 9, 0)), 1),
                    NotificationItem(Reminder("3", "pet-3", ReminderKind.VET, null, VetReason.GENERAL, null, LocalDateTime.of(2026, 11, 28, 10, 0)), 2),
                ),
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onReminderClick = {},
        )
    }
}
