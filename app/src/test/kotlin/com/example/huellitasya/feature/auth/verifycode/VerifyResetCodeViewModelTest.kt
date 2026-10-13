package com.example.huellitasya.feature.auth.verifycode

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.usecase.RequestPasswordResetCodeUseCase
import com.example.huellitasya.core.domain.usecase.VerifyPasswordResetCodeUseCase
import com.example.huellitasya.core.testing.FakePasswordResetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyResetCodeViewModelTest {

    // StandardTestDispatcher: la cuenta regresiva usa delay y se controla con advanceTimeBy.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakePasswordResetRepository()
    private val viewModel by lazy {
        VerifyResetCodeViewModel(
            savedStateHandle = SavedStateHandle(mapOf(VerifyResetCodeViewModel.EMAIL_ARG to "sa.company@hotmail.com")),
            verifyCode = VerifyPasswordResetCodeUseCase(repository),
            requestCode = RequestPasswordResetCodeUseCase(repository),
        )
    }

    @Test
    fun `solo acepta digitos y como maximo 4`() = runTest {
        viewModel.onAction(VerifyResetCodeAction.CodeChanged("12a3456"))

        assertEquals("1234", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `codigo incompleto muestra advertencia sin llamar al servidor`() = runTest {
        viewModel.onAction(VerifyResetCodeAction.CodeChanged("12"))

        viewModel.onAction(VerifyResetCodeAction.Submit)
        runCurrent()

        val feedback = viewModel.uiState.value.codeFeedback
        assertEquals(FieldStatus.Warning, feedback?.status)
        assertEquals(UiText.Resource(R.string.error_reset_code_incomplete), feedback?.message)
        assertTrue(repository.verifyCalls.isEmpty())
    }

    @Test
    fun `codigo incorrecto se marca en rojo`() = runTest {
        repository.verifyResult = AppResult.Failure(PasswordResetError.InvalidCode)
        viewModel.onAction(VerifyResetCodeAction.CodeChanged("2145"))

        viewModel.onAction(VerifyResetCodeAction.Submit)
        runCurrent()

        val feedback = viewModel.uiState.value.codeFeedback
        assertEquals(FieldStatus.Error, feedback?.status)
        assertEquals(PasswordResetError.InvalidCode.toUiText(), feedback?.message)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `codigo vencido se marca en rojo`() = runTest {
        repository.verifyResult = AppResult.Failure(PasswordResetError.ExpiredCode)
        viewModel.onAction(VerifyResetCodeAction.CodeChanged("2145"))

        viewModel.onAction(VerifyResetCodeAction.Submit)
        runCurrent()

        assertEquals(PasswordResetError.ExpiredCode.toUiText(), viewModel.uiState.value.codeFeedback?.message)
    }

    @Test
    fun `codigo correcto navega con el token`() = runTest {
        viewModel.onAction(VerifyResetCodeAction.CodeChanged("1145"))

        viewModel.events.test {
            viewModel.onAction(VerifyResetCodeAction.Submit)
            assertEquals(VerifyResetCodeEvent.NavigateToNewPassword("sa.company@hotmail.com", "token-123"), awaitItem())
        }
        assertEquals(listOf("sa.company@hotmail.com" to "1145"), repository.verifyCalls)
    }

    @Test
    fun `reenviar se habilita a los 60 segundos y reinicia la cuenta`() = runTest {
        runCurrent()
        assertFalse(viewModel.uiState.value.canResend)

        viewModel.onAction(VerifyResetCodeAction.Resend)
        runCurrent()
        assertTrue(repository.requestedEmails.isEmpty())

        advanceTimeBy(VerifyResetCodeViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 1)
        runCurrent()
        assertTrue(viewModel.uiState.value.canResend)

        viewModel.onAction(VerifyResetCodeAction.Resend)
        runCurrent()
        assertEquals(listOf("sa.company@hotmail.com"), repository.requestedEmails)
        assertEquals(VerifyResetCodeViewModel.RESEND_COOLDOWN_SECONDS, viewModel.uiState.value.resendSecondsLeft)
    }

    @Test
    fun `si el servidor pide esperar la cuenta usa ese tiempo`() = runTest {
        repository.requestResult = AppResult.Failure(PasswordResetError.TooManyRequests(retryAfterSeconds = 25))
        viewModel // crea el ViewModel: su cuenta regresiva arranca aquí
        runCurrent()
        advanceTimeBy(VerifyResetCodeViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 1)
        runCurrent()

        viewModel.onAction(VerifyResetCodeAction.Resend)
        runCurrent()

        assertEquals(25, viewModel.uiState.value.resendSecondsLeft)
    }
}
