package com.example.huellitasya.feature.profile.verifyemail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasTextButton
import com.example.huellitasya.core.designsystem.components.OtpCodeField
import com.example.huellitasya.core.domain.usecase.EMAIL_CHANGE_CODE_LENGTH
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.core.ui.UiText
import kotlinx.coroutines.launch

@Composable
internal fun VerifyEmailChangeRoute(
    onBackClick: () -> Unit,
    onEmailChanged: (String) -> Unit,
    viewModel: VerifyEmailChangeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is VerifyEmailChangeEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is VerifyEmailChangeEvent.EmailChanged -> onEmailChanged(event.email)
        }
    }

    VerifyEmailChangeScreen(
        newEmail = viewModel.newEmail,
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

/**
 * "Verifica tu correo" (spec 004). Mismos textos y casillas que la recuperación de contraseña
 * (Figma `VERIFICACION CORREO`, 139:822), con el estilo de Datos Personales (sin foto de cabecera).
 */
@Composable
internal fun VerifyEmailChangeScreen(
    newEmail: String,
    state: VerifyEmailChangeUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (VerifyEmailChangeAction) -> Unit,
    onBackClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(VerifyEmailChangeAction.Submit)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.3f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            HuellitasBackButton(
                onClick = onBackClick,
                contentDescription = stringResource(R.string.verify_email_back),
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.verify_email_title),
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.verify_email_subtitle, newEmail),
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.verify_email_code_label),
                    color = Color.Black.copy(alpha = 0.72f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OtpCodeField(
                    value = state.code,
                    onValueChange = { onAction(VerifyEmailChangeAction.CodeChanged(it)) },
                    length = EMAIL_CHANGE_CODE_LENGTH,
                    contentDescription = stringResource(R.string.verify_email_code_description),
                    modifier = Modifier.fillMaxWidth(),
                    status = state.codeFeedback?.status ?: FieldStatus.Default,
                    enabled = !state.isLoading,
                    onDone = submit,
                )
                state.codeFeedback?.message?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldMessage(text = message.asString(), status = state.codeFeedback.status)
                }
            }

            Spacer(modifier = Modifier.height(56.dp))
            HuellitasButton(
                text = stringResource(R.string.verify_email_button),
                onClick = submit,
                isLoading = state.isLoading,
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ResendCodeRow(
                secondsLeft = state.resendSecondsLeft,
                enabled = state.canResend,
                onResend = { onAction(VerifyEmailChangeAction.Resend) },
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** "¿No recibiste el código? Reenviar código" con cuenta regresiva. */
@Composable
private fun ResendCodeRow(secondsLeft: Int, enabled: Boolean, onResend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.verify_email_not_received),
            color = GrayText,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        HuellitasTextButton(
            text = if (secondsLeft > 0) {
                stringResource(R.string.verify_email_resend_in, secondsLeft)
            } else {
                stringResource(R.string.verify_email_resend)
            },
            onClick = onResend,
            enabled = enabled,
            color = if (enabled) LinkBlue else GrayText,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun VerifyEmailChangeScreenPreview() {
    HuellitasYATheme {
        VerifyEmailChangeScreen(
            newEmail = "nuevo@correo.com",
            state = VerifyEmailChangeUiState(code = "21", resendSecondsLeft = 42),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun VerifyEmailChangeScreenErrorPreview() {
    HuellitasYATheme {
        VerifyEmailChangeScreen(
            newEmail = "nuevo@correo.com",
            state = VerifyEmailChangeUiState(
                code = "2145",
                codeFeedback = FieldFeedback(FieldStatus.Error, UiText.Dynamic("El código ingresado no es correcto")),
                resendSecondsLeft = 0,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}
