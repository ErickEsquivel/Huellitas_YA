package com.example.huellitasya.feature.auth.forgotpassword

import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.usecase.GetBackendModeUseCase
import com.example.huellitasya.core.domain.usecase.RequestPasswordResetCodeUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.testing.FakeBackendModeProvider
import com.example.huellitasya.core.testing.FakePasswordResetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePasswordResetRepository()
    private val viewModel = viewModel(BackendMode.Blaze)

    private fun viewModel(mode: BackendMode) = ForgotPasswordViewModel(
        requestCode = RequestPasswordResetCodeUseCase(repository),
        validateEmail = ValidateEmailUseCase(),
        getBackendMode = GetBackendModeUseCase(FakeBackendModeProvider(mode)),
    )

    @Test
    fun `correo invalido muestra advertencia sin pedir el codigo`() {
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@"))

        viewModel.onAction(ForgotPasswordAction.Submit)

        val feedback = viewModel.uiState.value.emailFeedback
        assertEquals(FieldStatus.Warning, feedback?.status)
        assertEquals(ValidationError.InvalidEmail.toUiText(), feedback?.message)
        assertTrue(repository.requestedEmails.isEmpty())
    }

    @Test
    fun `pedir el codigo navega a verificar con el correo recortado`() = runTest {
        viewModel.onAction(ForgotPasswordAction.EmailChanged(" sa.company@hotmail.com "))

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            assertEquals(ForgotPasswordEvent.NavigateToVerifyCode("sa.company@hotmail.com"), awaitItem())
        }
        assertEquals(listOf("sa.company@hotmail.com"), repository.requestedEmails)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `si ya se envio un codigo hace poco tambien avanza`() = runTest {
        repository.requestResult = AppResult.Failure(PasswordResetError.TooManyRequests(retryAfterSeconds = 40))
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@hotmail.com"))

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            assertEquals(ForgotPasswordEvent.NavigateToVerifyCode("sa.company@hotmail.com"), awaitItem())
        }
    }

    @Test
    fun `error de red pide mostrar el overlay sin conexion`() = runTest {
        repository.requestResult = AppResult.Failure(PasswordResetError.Network)
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@hotmail.com"))

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            assertEquals(ForgotPasswordEvent.NetworkError, awaitItem())
        }
    }

    // --- Modo gratuito (spec 008): enlace en lugar de código

    @Test
    fun `en modo Blaze la pantalla pide el codigo`() {
        assertFalse(viewModel.uiState.value.usesLink)
    }

    @Test
    fun `en modo gratuito enviar el enlace pasa a Revisa tu correo sin navegar`() = runTest {
        val viewModel = viewModel(BackendMode.Free)
        assertTrue(viewModel.uiState.value.usesLink)
        viewModel.onAction(ForgotPasswordAction.EmailChanged(" sa.company@hotmail.com "))

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            expectNoEvents()
        }

        val state = viewModel.uiState.value
        assertEquals("sa.company@hotmail.com", state.linkSentTo)
        assertTrue(state.resendSecondsLeft > 0)
        assertFalse(state.canResend)
        assertEquals(listOf("sa.company@hotmail.com"), repository.requestedEmails)
    }

    @Test
    fun `en Revisa tu correo el boton vuelve a iniciar sesion`() = runTest {
        val viewModel = viewModel(BackendMode.Free)
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@hotmail.com"))
        viewModel.onAction(ForgotPasswordAction.Submit)

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            assertEquals(ForgotPasswordEvent.NavigateToLogin, awaitItem())
        }
        assertEquals(1, repository.requestedEmails.size)
    }

    @Test
    fun `reenviar el enlace espera la cuenta regresiva`() = runTest {
        val viewModel = viewModel(BackendMode.Free)
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@hotmail.com"))
        viewModel.onAction(ForgotPasswordAction.Submit)

        viewModel.onAction(ForgotPasswordAction.Resend)
        assertEquals(1, repository.requestedEmails.size)

        testScheduler.advanceTimeBy(61_000)
        assertTrue(viewModel.uiState.value.canResend)
        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Resend)
            assertEquals(
                ForgotPasswordEvent.ShowMessage(UiText.Resource(R.string.auth_forgot_link_resent)),
                awaitItem(),
            )
        }
        assertEquals(2, repository.requestedEmails.size)
    }

    @Test
    fun `en modo gratuito sin red muestra el overlay y no cambia de pantalla`() = runTest {
        repository.requestResult = AppResult.Failure(PasswordResetError.Network)
        val viewModel = viewModel(BackendMode.Free)
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@hotmail.com"))

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            assertEquals(ForgotPasswordEvent.NetworkError, awaitItem())
        }
        assertEquals(null, viewModel.uiState.value.linkSentTo)
    }
}
