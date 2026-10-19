package com.example.huellitasya.feature.settings

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.AlertRed
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LightGrayText
import com.example.huellitasya.core.designsystem.components.DeleteConfirmDialog
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.LoadingOverlay
import com.example.huellitasya.core.designsystem.components.PasswordConfirmDialog
import com.example.huellitasya.core.designsystem.components.ScreenHeader
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.core.ui.google.rememberGoogleIdTokenRequester
import kotlinx.coroutines.launch

@Composable
internal fun SettingsRoute(
    googleServerClientId: String?,
    onPersonalDataClick: () -> Unit,
    onSecurityClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleRequester = rememberGoogleIdTokenRequester(googleServerClientId)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateToSecurity -> onSecurityClick()
            SettingsEvent.LaunchGoogleReauth -> scope.launch {
                viewModel.onGoogleIdTokenResult(googleRequester.requestIdToken())
            }
            is SettingsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    SettingsScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onPersonalDataClick = onPersonalDataClick,
        onAction = viewModel::onAction,
    )
}

/** Figma "CONFIGURACION" (3452:1018) + fila "Eliminar cuenta" (spec 004). */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onPersonalDataClick: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.3f)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            ScreenHeader(
                title = stringResource(R.string.settings_title),
                titleFontSize = 32.sp,
                notificationsDescription = stringResource(R.string.settings_notifications),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_my_account),
                color = GrayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth().padding(start = 5.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsRow(
                    title = stringResource(R.string.settings_personal_data),
                    subtitle = stringResource(R.string.settings_personal_data_subtitle),
                    iconRes = R.drawable.ic_settings_personal,
                    onClick = onPersonalDataClick,
                )
                SettingsRow(
                    title = stringResource(R.string.settings_security),
                    subtitle = stringResource(R.string.settings_security_subtitle),
                    iconRes = R.drawable.ic_settings_security,
                    onClick = { onAction(SettingsAction.SecurityClick) },
                )
                SettingsRow(
                    title = stringResource(R.string.settings_delete_account),
                    subtitle = stringResource(R.string.settings_delete_account_subtitle),
                    iconRes = R.drawable.ic_settings_delete,
                    onClick = { onAction(SettingsAction.DeleteAccountClick) },
                    titleColor = AlertRed,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HuellitasButton(
                text = stringResource(R.string.settings_logout),
                onClick = { onAction(SettingsAction.Logout) },
                isLoading = state.isLoggingOut,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        LoadingOverlay(isVisible = state.isDeleting)
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (state.showDeleteConfirmation) {
        DeleteConfirmDialog(
            title = stringResource(R.string.settings_delete_title),
            message = stringResource(R.string.settings_delete_message),
            confirmText = stringResource(R.string.settings_delete_confirm),
            closeDescription = stringResource(R.string.settings_delete_close),
            imageDescription = stringResource(R.string.settings_delete_image_description),
            onConfirm = { onAction(SettingsAction.ConfirmDelete) },
            onDismiss = { onAction(SettingsAction.DismissDelete) },
        )
    }

    state.passwordPrompt?.let { prompt ->
        PasswordConfirmDialog(
            title = stringResource(R.string.settings_reauth_title),
            message = stringResource(R.string.settings_reauth_message),
            password = prompt.password,
            onPasswordChange = { onAction(SettingsAction.PasswordChanged(it)) },
            passwordLabel = stringResource(R.string.settings_reauth_password),
            showPasswordDescription = stringResource(R.string.settings_reauth_show_password),
            hidePasswordDescription = stringResource(R.string.settings_reauth_hide_password),
            confirmText = stringResource(R.string.settings_reauth_confirm),
            dismissText = stringResource(R.string.settings_reauth_cancel),
            onConfirm = { onAction(SettingsAction.ConfirmPassword) },
            onDismiss = { onAction(SettingsAction.DismissPassword) },
            status = prompt.feedback?.status ?: FieldStatus.Default,
            statusMessage = prompt.feedback?.message?.asString(),
            isLoading = prompt.isLoading,
        )
    }
}

/** Fila de "Mi cuenta": ícono de 45 dp, título, subtítulo y flecha (tarjeta de 62 dp de alto). */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    titleColor: Color = Color.Black,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .border(1.dp, BorderSubtle, shape)
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(45.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(iconRes), contentDescription = null)
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = LightGrayText,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_settings_chevron),
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SettingsScreenPreview() {
    HuellitasYATheme {
        SettingsScreen(
            state = SettingsUiState(),
            snackbarHostState = SnackbarHostState(),
            onPersonalDataClick = {},
            onAction = {},
        )
    }
}
