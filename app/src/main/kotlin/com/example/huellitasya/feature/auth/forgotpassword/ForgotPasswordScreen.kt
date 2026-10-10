package com.example.huellitasya.feature.auth.forgotpassword

import androidx.compose.foundation.layout.Spacer
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
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
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
) {
    val focusManager = LocalFocusManager.current

    AuthFormScaffold(
        hero = ForgotHero,
        badge = ForgotBadge,
        title = AnnotatedString(stringResource(R.string.auth_forgot_title)),
        titleFontSize = 32.sp,
        subtitle = stringResource(R.string.auth_forgot_subtitle),
        onBackClick = onBackClick,
        backTint = Color.White,
        snackbarHostState = snackbarHostState,
        bottomButton = {
            HuellitasButton(
                text = stringResource(R.string.auth_forgot_button),
                onClick = {
                    focusManager.clearFocus()
                    onAction(ForgotPasswordAction.Submit)
                },
                isLoading = state.isLoading,
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(319.dp),
            )
        },
    ) {
        Spacer(modifier = Modifier.height(35.dp))
        HuellitasTextField(
            value = state.email,
            onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
            label = stringResource(R.string.auth_field_email),
            status = state.emailFeedback?.status ?: FieldStatus.Default,
            message = state.emailFeedback?.message?.asString(),
            enabled = !state.isLoading,
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
        if (state.isEmailSent) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.auth_forgot_success),
                color = GreenPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
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
