package com.example.huellitasya.feature.auth.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
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
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.LinkBlue
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasPasswordField
import com.example.huellitasya.core.designsystem.components.HuellitasTextButton
import com.example.huellitasya.core.designsystem.components.HuellitasTextField
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.MissingFieldsFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.auth.components.AuthBadgeSpec
import com.example.huellitasya.feature.auth.components.AuthFormScaffold
import com.example.huellitasya.feature.auth.components.AuthHeroSpec
import com.example.huellitasya.feature.auth.components.highlightedTitle
import kotlinx.coroutines.launch

// Figma "INICIO SESION CORREO" (158:1303)
private val LoginHero = AuthHeroSpec(R.drawable.img_auth_login_hero, 440f, -14f, -104.5f, 353f)
private val LoginBadge = AuthBadgeSpec(R.drawable.img_auth_badge_clipboard, 323f, DpSize(141.dp, 87.dp))

@Composable
internal fun LoginRoute(
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onNetworkError: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LoginEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            LoginEvent.NetworkError -> onNetworkError()
        }
    }

    LoginScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onForgotPasswordClick = onForgotPasswordClick,
    )
}

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (LoginAction) -> Unit,
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    forceKeyboardLayout: Boolean = false,
) {
    val focusManager = LocalFocusManager.current

    AuthFormScaffold(
        hero = LoginHero,
        badge = LoginBadge,
        title = highlightedTitle(
            stringResource(R.string.auth_login_title),
            stringResource(R.string.auth_login_title_highlight),
        ),
        subtitle = stringResource(R.string.auth_login_subtitle),
        subtitleTopSpacing = 10.dp,
        onBackClick = onBackClick,
        forceKeyboardLayout = forceKeyboardLayout,
        snackbarHostState = snackbarHostState,
        bottomButton = {
            HuellitasButton(
                text = stringResource(R.string.auth_login_button),
                onClick = {
                    focusManager.clearFocus()
                    onAction(LoginAction.Submit)
                },
                isLoading = state.isLoading,
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(319.dp),
            )
        },
    ) { isCompact ->
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 23.dp))
        HuellitasTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.EmailChanged(it)) },
            label = stringResource(R.string.auth_field_email),
            status = state.emailFeedback?.status ?: FieldStatus.Default,
            message = state.emailFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(modifier = Modifier.height(8.dp))
        HuellitasPasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = stringResource(R.string.auth_field_password),
            showPasswordDescription = stringResource(R.string.auth_show_password),
            hidePasswordDescription = stringResource(R.string.auth_hide_password),
            status = state.passwordFeedback?.status ?: FieldStatus.Default,
            message = state.passwordFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onAction(LoginAction.Submit)
            }),
        )
        state.formFeedback?.let { feedback ->
            feedback.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                FieldMessage(text = message.asString(), status = feedback.status)
            }
        }
        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 32.dp))
        HuellitasTextButton(
            text = stringResource(R.string.auth_login_forgot_password),
            onClick = onForgotPasswordClick,
            enabled = !state.isLoading,
            color = LinkBlue.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun LoginScreenPreview() {
    HuellitasYATheme {
        LoginScreen(
            state = LoginUiState(email = "Sa.company@hotmail.com", password = "secreto"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onForgotPasswordClick = {},
        )
    }
}

@Preview(name = "Teléfono compacto", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoginScreenCompactPreview() {
    HuellitasYATheme {
        LoginScreen(
            state = LoginUiState(email = "Sa.company@hotmail.com", password = "secreto"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onForgotPasswordClick = {},
        )
    }
}

@Preview(name = "Teclado abierto", showBackground = true, widthDp = 412, heightDp = 420)
@Composable
private fun LoginScreenKeyboardPreview() {
    HuellitasYATheme {
        LoginScreen(
            state = LoginUiState(email = "Sa.company@hotmail.com"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onForgotPasswordClick = {},
            forceKeyboardLayout = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun LoginScreenMissingFieldsPreview() {
    HuellitasYATheme {
        LoginScreen(
            state = LoginUiState(
                emailFeedback = FieldFeedback(FieldStatus.Warning),
                passwordFeedback = FieldFeedback(FieldStatus.Warning),
                formFeedback = MissingFieldsFeedback,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onForgotPasswordClick = {},
        )
    }
}
