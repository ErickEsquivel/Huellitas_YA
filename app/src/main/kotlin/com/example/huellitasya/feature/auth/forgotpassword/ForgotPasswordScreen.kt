package com.example.huellitasya.feature.auth.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.huellitasya.core.designsystem.LinkBlue
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasTextButton
import com.example.huellitasya.core.designsystem.components.HuellitasTextField
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.auth.components.AuthBadgeSpec
import com.example.huellitasya.feature.auth.components.AuthFormScaffold
import com.example.huellitasya.feature.auth.components.AuthHeroSpec
import kotlinx.coroutines.launch

// Figma "OLVIDO CONTRASEÑA" (141:1069)
private val ForgotHero = AuthHeroSpec(R.drawable.img_auth_forgot_hero, 451.3f, -14f, -140f, 337f)
private val ForgotBadge = AuthBadgeSpec(R.drawable.img_auth_badge_lock, 293f, DpSize(143.dp, 88.dp))

@Composable
internal fun ForgotPasswordRoute(
    onBackClick: () -> Unit,
    onCodeSent: (email: String) -> Unit,
    onBackToLogin: () -> Unit,
    onNetworkError: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ForgotPasswordEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is ForgotPasswordEvent.NavigateToVerifyCode -> onCodeSent(event.email)
            ForgotPasswordEvent.NavigateToLogin -> onBackToLogin()
            ForgotPasswordEvent.NetworkError -> onNetworkError()
        }
    }

    ForgotPasswordScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ForgotPasswordAction) -> Unit,
    onBackClick: () -> Unit,
    forceKeyboardLayout: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    // Modo gratuito (spec 008): después de enviar el enlace, la pantalla pasa a "Revisa tu correo".
    val linkSentTo = state.linkSentTo
    val title = if (linkSentTo != null) R.string.auth_forgot_link_sent_title else R.string.auth_forgot_title
    val subtitle = when {
        linkSentTo != null -> stringResource(R.string.auth_forgot_link_sent_subtitle, linkSentTo)
        state.usesLink -> stringResource(R.string.auth_forgot_subtitle_link)
        else -> stringResource(R.string.auth_forgot_subtitle)
    }
    val buttonText = when {
        linkSentTo != null -> R.string.auth_forgot_back_to_login
        state.usesLink -> R.string.auth_forgot_button_link
        else -> R.string.auth_forgot_button
    }

    AuthFormScaffold(
        hero = ForgotHero,
        badge = ForgotBadge,
        title = AnnotatedString(stringResource(title)),
        titleFontSize = 32.sp,
        compactTitleFontSize = 24.sp,
        subtitle = subtitle,
        onBackClick = onBackClick,
        backTint = Color.White,
        forceKeyboardLayout = forceKeyboardLayout,
        snackbarHostState = snackbarHostState,
        bottomButton = {
            HuellitasButton(
                text = stringResource(buttonText),
                onClick = {
                    focusManager.clearFocus()
                    onAction(ForgotPasswordAction.Submit)
                },
                isLoading = state.isLoading,
                containerColor = if (state.isComplete || linkSentTo != null) GreenAction else ButtonIncomplete,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(319.dp),
            )
        },
    ) { isCompact ->
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 35.dp))
        if (linkSentTo != null) {
            ResendLinkRow(
                secondsLeft = state.resendSecondsLeft,
                enabled = state.canResend,
                onResend = { onAction(ForgotPasswordAction.Resend) },
            )
            return@AuthFormScaffold
        }
        HuellitasTextField(
            value = state.email,
            onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
            label = stringResource(R.string.auth_field_email),
            status = state.emailFeedback?.status ?: FieldStatus.Default,
            message = state.emailFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onAction(ForgotPasswordAction.Submit)
            }),
        )
        state.formFeedback?.let { feedback ->
            feedback.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                FieldMessage(text = message.asString(), status = feedback.status)
            }
        }
    }
}

/** "¿No te llegó? Reenviar enlace" con cuenta regresiva (modo gratuito, spec 008). */
@Composable
private fun ResendLinkRow(secondsLeft: Int, enabled: Boolean, onResend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.auth_forgot_link_not_received),
            color = GrayText,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        HuellitasTextButton(
            text = if (secondsLeft > 0) {
                stringResource(R.string.auth_verify_resend_in, secondsLeft)
            } else {
                stringResource(R.string.auth_forgot_link_resend)
            },
            onClick = onResend,
            enabled = enabled,
            color = if (enabled) LinkBlue else GrayText,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ForgotPasswordScreenPreview() {
    HuellitasYATheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Revisa tu correo (modo gratuito)", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ForgotPasswordLinkSentPreview() {
    HuellitasYATheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(
                email = "sa.company@hotmail.com",
                usesLink = true,
                linkSentTo = "sa.company@hotmail.com",
                resendSecondsLeft = 42,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Teléfono compacto", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ForgotPasswordScreenCompactPreview() {
    HuellitasYATheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Teclado abierto", showBackground = true, widthDp = 412, heightDp = 420)
@Composable
private fun ForgotPasswordScreenKeyboardPreview() {
    HuellitasYATheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(email = "Sa.company@"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            forceKeyboardLayout = true,
        )
    }
}
