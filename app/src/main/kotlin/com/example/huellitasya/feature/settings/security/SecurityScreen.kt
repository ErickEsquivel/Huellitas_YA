package com.example.huellitasya.feature.settings.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.ButtonIncomplete
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasPasswordField
import com.example.huellitasya.core.designsystem.components.PasswordRequirementRow
import com.example.huellitasya.core.domain.validation.PasswordRequirements
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.core.ui.UiText
import kotlinx.coroutines.launch

@Composable
internal fun SecurityRoute(
    onBackClick: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SecurityEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    SecurityScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

/** Figma `CONFIGURACION` → Seguridad (3263:1086; con error, 3278:1170). */
@Composable
internal fun SecurityScreen(
    state: SecurityUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (SecurityAction) -> Unit,
    onBackClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(SecurityAction.Submit)
    }
    val showPassword = stringResource(R.string.security_show_password)
    val hidePassword = stringResource(R.string.security_hide_password)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.22f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Edge-to-edge: el botón no debe quedar debajo de la barra de navegación ni del teclado.
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            HuellitasBackButton(
                onClick = onBackClick,
                contentDescription = stringResource(R.string.security_back),
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.security_title),
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(R.string.security_subtitle),
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(40.dp))

            HuellitasPasswordField(
                value = state.currentPassword,
                onValueChange = { onAction(SecurityAction.CurrentPasswordChanged(it)) },
                label = stringResource(R.string.security_current_password),
                showPasswordDescription = showPassword,
                hidePasswordDescription = hidePassword,
                status = state.currentFeedback?.status ?: FieldStatus.Default,
                message = state.currentFeedback?.message?.asString(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HuellitasPasswordField(
                value = state.newPassword,
                onValueChange = { onAction(SecurityAction.NewPasswordChanged(it)) },
                label = stringResource(R.string.security_new_password),
                showPasswordDescription = showPassword,
                hidePasswordDescription = hidePassword,
                status = state.newFeedback?.status ?: FieldStatus.Default,
                message = state.newFeedback?.message?.asString(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HuellitasPasswordField(
                value = state.confirmation,
                onValueChange = { onAction(SecurityAction.ConfirmationChanged(it)) },
                label = stringResource(R.string.security_confirm_password),
                showPasswordDescription = showPassword,
                hidePasswordDescription = hidePassword,
                status = state.confirmationFeedback?.status ?: FieldStatus.Default,
                message = state.confirmationFeedback?.message?.asString(),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Spacer(modifier = Modifier.height(18.dp))
            PasswordChecklist(requirements = state.requirements)

            state.formFeedback?.message?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                FieldMessage(
                    text = message.asString(),
                    status = state.formFeedback.status,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            HuellitasButton(
                text = stringResource(R.string.security_button),
                onClick = submit,
                isLoading = state.isLoading,
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(width = 279.dp, height = 48.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Checklist en vivo de la contraseña nueva (mismas reglas que el registro, spec 002). */
@Composable
private fun PasswordChecklist(requirements: PasswordRequirements) {
    val met = stringResource(R.string.security_rule_met)
    val unmet = stringResource(R.string.security_rule_unmet)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(
            R.string.security_rule_length to requirements.hasMinLength,
            R.string.security_rule_uppercase to requirements.hasUppercase,
            R.string.security_rule_digit to requirements.hasDigitOrSymbol,
            R.string.security_rule_match to requirements.matchesConfirmation,
        ).forEach { (text, isMet) ->
            PasswordRequirementRow(
                text = stringResource(text),
                isMet = isMet,
                stateDescription = if (isMet) met else unmet,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SecurityScreenPreview() {
    HuellitasYATheme {
        SecurityScreen(
            state = SecurityUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SecurityScreenWrongPasswordPreview() {
    HuellitasYATheme {
        SecurityScreen(
            state = SecurityUiState(
                currentPassword = "Huellitas1",
                newPassword = "Nueva2025",
                confirmation = "Nueva2025",
                requirements = PasswordRequirements(
                    hasMinLength = true,
                    hasUppercase = true,
                    hasDigitOrSymbol = true,
                    matchesConfirmation = true,
                ),
                currentFeedback = FieldFeedback(FieldStatus.Error, UiText.Dynamic("La contraseña ingresada no es correcta")),
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}
