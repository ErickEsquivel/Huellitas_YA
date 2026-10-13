package com.example.huellitasya.feature.profile.verifyemail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.usecase.ConfirmEmailChangeUseCase
import com.example.huellitasya.core.domain.usecase.RequestEmailChangeCodeUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeEmailChangeRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailChangeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val authRepository = FakeAuthRepository()
    private val emailChangeRepository = FakeEmailChangeRepository()

    private fun viewModel() = VerifyEmailChangeViewModel(
        savedStateHandle = SavedStateHandle(mapOf(VerifyEmailChangeViewModel.NEW_EMAIL_ARG to "nuevo@correo.com")),
        confirmEmailChange = ConfirmEmailChangeUseCase(emailChangeRepository, authRepository),
        requestEmailChangeCode = RequestEmailChangeCodeUseCase(emailChangeRepository),
    )

    @Test
    fun `solo acepta digitos y como maximo cuatro`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(VerifyEmailChangeAction.CodeChanged("12a345"))

        assertEquals("1234", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `codigo incompleto se marca sin llamar al backend`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(VerifyEmailChangeAction.CodeChanged("12"))

        viewModel.onAction(VerifyEmailChangeAction.Submit)

        assertEquals(FieldStatus.Warning, viewModel.uiState.value.codeFeedback?.status)
        assertTrue(emailChangeRepository.confirmedCodes.isEmpty())
    }

    @Test
    fun `codigo correcto devuelve el correo nuevo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(VerifyEmailChangeAction.CodeChanged("4821"))

        viewModel.events.test {
            viewModel.onAction(VerifyEmailChangeAction.Submit)
            assertEquals(VerifyEmailChangeEvent.EmailChanged("nuevo@correo.com"), awaitItem())
        }
        assertEquals(listOf("4821"), emailChangeRepository.confirmedCodes)
    }

    @Test
    fun `codigo incorrecto se marca en rojo`() = runTest(mainDispatcherRule.testDispatcher) {
        emailChangeRepository.confirmResult = AppResult.Failure(EmailChangeError.InvalidCode)
        val viewModel = viewModel()
        viewModel.onAction(VerifyEmailChangeAction.CodeChanged("0000"))

        viewModel.onAction(VerifyEmailChangeAction.Submit)
        testScheduler.runCurrent()

        assertEquals(FieldStatus.Error, viewModel.uiState.value.codeFeedback?.status)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `reenviar solo se habilita a los 60 segundos`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        testScheduler.runCurrent()

        viewModel.onAction(VerifyEmailChangeAction.Resend)
        assertTrue(emailChangeRepository.requestedEmails.isEmpty())

        advanceTimeBy(60_001)
        assertTrue(viewModel.uiState.value.canResend)
        viewModel.events.test {
            viewModel.onAction(VerifyEmailChangeAction.Resend)
            assertEquals(
                VerifyEmailChangeEvent.ShowMessage(UiText.Resource(R.string.verify_email_resent)),
                awaitItem(),
            )
        }
        assertEquals(listOf("nuevo@correo.com"), emailChangeRepository.requestedEmails)
    }

    @Test
    fun `reenviar con la sesion vieja pide volver a guardar el correo`() = runTest(mainDispatcherRule.testDispatcher) {
        emailChangeRepository.requestResults = listOf(AppResult.Failure(EmailChangeError.RequiresRecentLogin))
        val viewModel = viewModel()
        advanceTimeBy(60_001)

        viewModel.events.test {
            viewModel.onAction(VerifyEmailChangeAction.Resend)
            assertEquals(
                VerifyEmailChangeEvent.ShowMessage(UiText.Resource(R.string.verify_email_reauth_needed)),
                awaitItem(),
            )
        }
    }
}
