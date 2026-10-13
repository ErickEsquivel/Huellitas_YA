package com.example.huellitasya.feature.auth.verifycode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LinkBlue
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasTextButton
import com.example.huellitasya.core.designsystem.components.OtpCodeField
import com.example.huellitasya.core.domain.usecase.PASSWORD_RESET_CODE_LENGTH
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.feature.auth.components.AuthBadgeSpec
import com.example.huellitasya.feature.auth.components.AuthFormScaffold
import com.example.huellitasya.feature.auth.components.AuthHeroSpec
import kotlinx.coroutines.launch

// Figma "VERIFICACION CORREO" (139:822)
private val VerifyHero = AuthHeroSpec(R.drawable.img_auth_verify_hero, 457.7f, -27.7f, -147.1f, 345f)
private val VerifyBadge = AuthBadgeSpec(R.drawable.img_auth_badge_lock, 301f, DpSize(143.dp, 88.dp))

@Composable
internal fun VerifyResetCodeRoute(
    onBackClick: () -> Unit,
    onCodeVerified: (email: String, resetToken: String) -> Unit,
    onNetworkError: () -> Unit,
    viewModel: VerifyResetCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is VerifyResetCodeEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is VerifyResetCodeEvent.NavigateToNewPassword -> onCodeVerified(event.email, event.resetToken)
            VerifyResetCodeEvent.NetworkError -> onNetworkError()
        }
    }

    VerifyResetCodeScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
internal fun VerifyResetCodeScreen(
    state: VerifyResetCodeUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (VerifyResetCodeAction) -> Unit,
    onBackClick: () -> Unit,
    forceKeyboardLayout: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(VerifyResetCodeAction.Submit)
    }

    AuthFormScaffold(
        hero = VerifyHero,
        badge = VerifyBadge,
        title = AnnotatedString(stringResource(R.string.auth_verify_title)),
        titleFontSize = 32.sp,
        compactTitleFontSize = 24.sp,
        subtitle = stringResource(R.string.auth_verify_subtitle),
        subtitleFontSize = 16.sp,
        subtitleColor = Color.Black.copy(alpha = 0.5f),
        subtitleTopSpacing = 8.dp,
        onBackClick = onBackClick,
        forceKeyboardLayout = forceKeyboardLayout,
        snackbarHostState = snackbarHostState,
        bottomButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HuellitasButton(
                    text = stringResource(R.string.auth_verify_button),
                    onClick = submit,
                    isLoading = state.isLoading,
                    containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(319.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResendCodeRow(
                    secondsLeft = state.resendSecondsLeft,
                    enabled = state.canResend,
                    onResend = { onAction(VerifyResetCodeAction.Resend) },
                )
            }
        },
    ) { isCompact ->
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 40.dp))
        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                text = stringResource(R.string.auth_verify_code_label),
                color = Color.Black.copy(alpha = 0.72f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 26.dp))
            OtpCodeField(
                value = state.code,
                onValueChange = { onAction(VerifyResetCodeAction.CodeChanged(it)) },
                length = PASSWORD_RESET_CODE_LENGTH,
                contentDescription = stringResource(R.string.auth_verify_code_description),
                modifier = Modifier.fillMaxWidth(),
                status = state.codeFeedback?.status ?: FieldStatus.Default,
                enabled = !state.isLoading,
                onDone = submit,
            )
            state.codeFeedback?.let { feedback ->
                feedback.message?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldMessage(text = message.asString(), status = feedback.status)
                }
            }
        }
    }
}

/** "¿No recibiste el código? Reenviar código" con cuenta regresiva. */
@Composable
private fun ResendCodeRow(secondsLeft: Int, enabled: Boolean, onResend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.auth_verify_not_received),
            color = GrayText,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        HuellitasTextButton(
            text = if (secondsLeft > 0) {
                stringResource(R.string.auth_verify_resend_in, secondsLeft)
            } else {
                stringResource(R.string.auth_verify_resend)
            },
            onClick = onResend,
            enabled = enabled,
            color = if (enabled) LinkBlue else GrayText,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun VerifyResetCodeScreenPreview() {
    HuellitasYATheme {
        VerifyResetCodeScreen(
            state = VerifyResetCodeUiState(code = "21", resendSecondsLeft = 42),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Código incorrecto", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun VerifyResetCodeScreenErrorPreview() {
    HuellitasYATheme {
        VerifyResetCodeScreen(
            state = VerifyResetCodeUiState(
                code = "2145",
                codeFeedback = FieldFeedback(FieldStatus.Error, UiText.Resource(R.string.error_reset_code_invalid)),
                resendSecondsLeft = 0,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Teclado abierto", showBackground = true, widthDp = 412, heightDp = 420)
@Composable
private fun VerifyResetCodeScreenKeyboardPreview() {
    HuellitasYATheme {
        VerifyResetCodeScreen(
            state = VerifyResetCodeUiState(code = "21"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            forceKeyboardLayout = true,
        )
    }
}
