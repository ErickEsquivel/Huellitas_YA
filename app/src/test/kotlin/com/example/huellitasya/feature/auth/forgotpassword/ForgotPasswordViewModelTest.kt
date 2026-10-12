package com.example.huellitasya.feature.auth.forgotpassword

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.RequestPasswordResetCodeUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.testing.FakePasswordResetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
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
    private val viewModel = ForgotPasswordViewModel(
        requestCode = RequestPasswordResetCodeUseCase(repository),
        validateEmail = ValidateEmailUseCase(),
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
}
