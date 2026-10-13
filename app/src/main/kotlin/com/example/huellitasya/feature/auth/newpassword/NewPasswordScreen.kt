package com.example.huellitasya.feature.auth.newpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasPasswordField
import com.example.huellitasya.core.designsystem.components.PasswordRequirementRow
import com.example.huellitasya.core.domain.validation.PasswordRequirements
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.auth.components.AuthBadgeSpec
import com.example.huellitasya.feature.auth.components.AuthFormScaffold
import com.example.huellitasya.feature.auth.components.AuthHeroSpec
import kotlinx.coroutines.launch

// Figma "CREAR CONTRASEÑA" (141:996)
private val NewPasswordHero = AuthHeroSpec(R.drawable.img_auth_newpassword_hero, 433.3f, -9.8f, -141.8f, 319f)
private val NewPasswordBadge = AuthBadgeSpec(R.drawable.img_auth_badge_lock, 282f, DpSize(143.dp, 88.dp))

@Composable
internal fun NewPasswordRoute(
    onBackClick: () -> Unit,
    onPasswordReset: () -> Unit,
    onNetworkError: () -> Unit,
    viewModel: NewPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NewPasswordEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            NewPasswordEvent.PasswordReset -> onPasswordReset()
            NewPasswordEvent.NetworkError -> onNetworkError()
        }
    }

    NewPasswordScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
internal fun NewPasswordScreen(
    state: NewPasswordUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (NewPasswordAction) -> Unit,
    onBackClick: () -> Unit,
    forceKeyboardLayout: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val showPassword = stringResource(R.string.auth_show_password)
    val hidePassword = stringResource(R.string.auth_hide_password)
    val submit = {
        focusManager.clearFocus()
        onAction(NewPasswordAction.Submit)
    }

    AuthFormScaffold(
        hero = NewPasswordHero,
        badge = NewPasswordBadge,
        title = AnnotatedString(stringResource(R.string.auth_newpassword_title)),
        titleFontSize = 32.sp,
        compactTitleFontSize = 24.sp,
        subtitle = null,
        onBackClick = onBackClick,
        forceKeyboardLayout = forceKeyboardLayout,
        snackbarHostState = snackbarHostState,
        bottomButton = {
            HuellitasButton(
                text = stringResource(R.string.auth_newpassword_button),
                onClick = submit,
                isLoading = state.isLoading,
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(319.dp),
            )
        },
    ) { isCompact ->
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 24.dp))
        HuellitasPasswordField(
            value = state.password,
            onValueChange = { onAction(NewPasswordAction.PasswordChanged(it)) },
            label = stringResource(R.string.auth_newpassword_field),
            showPasswordDescription = showPassword,
            hidePasswordDescription = hidePassword,
            status = state.passwordFeedback?.status ?: FieldStatus.Default,
            message = state.passwordFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
        )
        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 14.dp))
        HuellitasPasswordField(
            value = state.confirmation,
            onValueChange = { onAction(NewPasswordAction.ConfirmationChanged(it)) },
            label = stringResource(R.string.auth_newpassword_confirm),
            showPasswordDescription = showPassword,
            hidePasswordDescription = hidePassword,
            status = state.confirmationFeedback?.status ?: FieldStatus.Default,
            message = state.confirmationFeedback?.message?.asString(),
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            compact = isCompact,
        )
        state.formFeedback?.let { feedback ->
            feedback.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                FieldMessage(text = message.asString(), status = feedback.status)
            }
        }
        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 18.dp))
        PasswordChecklist(requirements = state.requirements)
    }
}

@Composable
private fun PasswordChecklist(requirements: PasswordRequirements) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val met = stringResource(R.string.auth_newpassword_rule_met)
        val unmet = stringResource(R.string.auth_newpassword_rule_unmet)
        PasswordRequirementRow(
            stringResource(R.string.auth_newpassword_rule_length),
            requirements.hasMinLength,
            if (requirements.hasMinLength) met else unmet,
        )
        PasswordRequirementRow(
            stringResource(R.string.auth_newpassword_rule_uppercase),
            requirements.hasUppercase,
            if (requirements.hasUppercase) met else unmet,
        )
        PasswordRequirementRow(
            stringResource(R.string.auth_newpassword_rule_digit),
            requirements.hasDigitOrSymbol,
            if (requirements.hasDigitOrSymbol) met else unmet,
        )
        PasswordRequirementRow(
            stringResource(R.string.auth_newpassword_rule_match),
            requirements.matchesConfirmation,
            if (requirements.matchesConfirmation) met else unmet,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NewPasswordScreenPreview() {
    HuellitasYATheme {
        NewPasswordScreen(
            state = NewPasswordUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Requisitos cumplidos", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun NewPasswordScreenMetPreview() {
    HuellitasYATheme {
        NewPasswordScreen(
            state = NewPasswordUiState(
                password = "Huellitas1",
                confirmation = "Huellitas1",
                requirements = PasswordRequirements(true, true, true, true),
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Teclado abierto", showBackground = true, widthDp = 412, heightDp = 480)
@Composable
private fun NewPasswordScreenKeyboardPreview() {
    HuellitasYATheme {
        NewPasswordScreen(
            state = NewPasswordUiState(password = "Huellitas"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            forceKeyboardLayout = true,
        )
    }
}
