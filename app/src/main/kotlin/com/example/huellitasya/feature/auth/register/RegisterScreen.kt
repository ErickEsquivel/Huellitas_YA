package com.example.huellitasya.feature.auth.register

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
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
import com.example.huellitasya.core.designsystem.WarningOrange
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasPasswordField
import com.example.huellitasya.core.designsystem.components.HuellitasTextField
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.auth.components.AuthBadgeSpec
import com.example.huellitasya.feature.auth.components.AuthFormScaffold
import com.example.huellitasya.feature.auth.components.AuthHeroSpec
import com.example.huellitasya.feature.auth.components.highlightedTitle
import kotlinx.coroutines.launch

// Figma "REGISTRO" (158:1329)
private val RegisterHero = AuthHeroSpec(R.drawable.img_auth_register_hero, 426f, -8.6f, -140.5f, 306f)
private val RegisterBadge = AuthBadgeSpec(R.drawable.img_auth_badge_clipboard, 247f, DpSize(141.dp, 87.dp))

private val CheckboxFill = Color(0xFFEBFFEE)

@Composable
internal fun RegisterRoute(
    onBackClick: () -> Unit,
    onNetworkError: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RegisterEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            RegisterEvent.NetworkError -> onNetworkError()
        }
    }

    RegisterScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
internal fun RegisterScreen(
    state: RegisterUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (RegisterAction) -> Unit,
    onBackClick: () -> Unit,
    forceKeyboardLayout: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val showPassword = stringResource(R.string.auth_show_password)
    val hidePassword = stringResource(R.string.auth_hide_password)

    AuthFormScaffold(
        hero = RegisterHero,
        badge = RegisterBadge,
        title = highlightedTitle(
            stringResource(R.string.auth_register_title),
            stringResource(R.string.auth_register_title_highlight),
        ),
        subtitle = stringResource(R.string.auth_register_subtitle),
        onBackClick = onBackClick,
        forceKeyboardLayout = forceKeyboardLayout,
        snackbarHostState = snackbarHostState,
        bottomButton = {
            HuellitasButton(
                text = stringResource(R.string.auth_register_button),
                onClick = {
                    focusManager.clearFocus()
                    onAction(RegisterAction.Submit)
                },
                isLoading = state.isLoading,
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(319.dp),
            )
        },
    ) { isCompact ->
        val fieldSpacing = if (isCompact) 4.dp else 8.dp
        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 30.dp))
        HuellitasTextField(
            value = state.name,
            onValueChange = { onAction(RegisterAction.NameChanged(it)) },
            label = stringResource(R.string.auth_field_name),
            status = state.nameFeedback.status,
            message = state.nameFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(modifier = Modifier.height(fieldSpacing))
        HuellitasTextField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
            label = stringResource(R.string.auth_field_email),
            status = state.emailFeedback.status,
            message = state.emailFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(modifier = Modifier.height(fieldSpacing))
        HuellitasPasswordField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
            label = stringResource(R.string.auth_field_password),
            showPasswordDescription = showPassword,
            hidePasswordDescription = hidePassword,
            status = state.passwordFeedback.status,
            message = state.passwordFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
        )
        Spacer(modifier = Modifier.height(fieldSpacing))
        HuellitasPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(RegisterAction.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.auth_field_confirm_password),
            showPasswordDescription = showPassword,
            hidePasswordDescription = hidePassword,
            status = state.confirmPasswordFeedback.status,
            message = state.confirmPasswordFeedback?.message?.asString(),
            enabled = !state.isLoading,
            compact = isCompact,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        state.formFeedback?.let { feedback ->
            feedback.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                FieldMessage(text = message.asString(), status = feedback.status)
            }
        }
        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 24.dp))
        TermsCheckbox(
            checked = state.termsAccepted,
            onCheckedChange = { onAction(RegisterAction.TermsChanged(it)) },
            isWarning = state.termsFeedback != null,
            enabled = !state.isLoading,
        )
        state.termsFeedback?.let { feedback ->
            feedback.message?.let { message ->
                Spacer(modifier = Modifier.height(6.dp))
                FieldMessage(text = message.asString(), status = feedback.status)
            }
        }
    }
}

private val FieldFeedback?.status: FieldStatus
    get() = this?.status ?: FieldStatus.Default

/** "☐ Acepta los Términos y Condiciones" (Figma: casilla 12x11 con fondo verde muy claro). */
@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isWarning: Boolean,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 11.dp)
                .background(if (checked) GreenAction else CheckboxFill)
                .border(1.dp, if (isWarning) WarningOrange else Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            // Visto dibujado: el carácter "✓" no está en Nunito y salía recortado dentro de 12x11 dp.
            if (checked) {
                Canvas(modifier = Modifier.size(width = 8.dp, height = 7.dp)) {
                    val check = Path().apply {
                        moveTo(size.width * 0.05f, size.height * 0.55f)
                        lineTo(size.width * 0.38f, size.height * 0.9f)
                        lineTo(size.width * 0.95f, size.height * 0.1f)
                    }
                    drawPath(
                        path = check,
                        color = Color.White,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.auth_register_terms_prefix))
                withStyle(SpanStyle(color = LinkBlue)) { append(stringResource(R.string.auth_register_terms)) }
                append(stringResource(R.string.auth_register_terms_and))
                withStyle(SpanStyle(color = LinkBlue)) { append(stringResource(R.string.auth_register_conditions)) }
            },
            color = Color.Black.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RegisterScreenPreview() {
    HuellitasYATheme {
        RegisterScreen(
            state = RegisterUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Teléfono compacto", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun RegisterScreenCompactPreview() {
    HuellitasYATheme {
        RegisterScreen(
            state = RegisterUiState(),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Teclado abierto", showBackground = true, widthDp = 412, heightDp = 480)
@Composable
private fun RegisterScreenKeyboardPreview() {
    HuellitasYATheme {
        RegisterScreen(
            state = RegisterUiState(name = "Santiago Cruz"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            forceKeyboardLayout = true,
        )
    }
}
