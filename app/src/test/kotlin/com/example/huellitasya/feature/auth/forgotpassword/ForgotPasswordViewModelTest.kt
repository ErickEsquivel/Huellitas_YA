package com.example.huellitasya.feature.auth.forgotpassword

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.SendPasswordResetUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
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

    private val authRepository = FakeAuthRepository()
    private val viewModel = ForgotPasswordViewModel(
        sendPasswordReset = SendPasswordResetUseCase(authRepository),
        validateEmail = ValidateEmailUseCase(),
    )

    @Test
    fun `correo invalido muestra advertencia sin enviar`() {
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@"))

        viewModel.onAction(ForgotPasswordAction.Submit)

        val feedback = viewModel.uiState.value.emailFeedback
        assertEquals(FieldStatus.Warning, feedback?.status)
        assertEquals(ValidationError.InvalidEmail.toUiText(), feedback?.message)
        assertTrue(authRepository.passwordResetEmails.isEmpty())
    }

    @Test
    fun `envio exitoso muestra la confirmacion generica`() {
        viewModel.onAction(ForgotPasswordAction.EmailChanged(" sa.company@hotmail.com "))

        viewModel.onAction(ForgotPasswordAction.Submit)

        assertTrue(viewModel.uiState.value.isEmailSent)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("sa.company@hotmail.com"), authRepository.passwordResetEmails)
    }

    @Test
    fun `error de red pide mostrar el overlay sin conexion`() = runTest {
        authRepository.passwordResetResult = AppResult.Failure(AuthError.Network)
        viewModel.onAction(ForgotPasswordAction.EmailChanged("sa.company@hotmail.com"))

        viewModel.events.test {
            viewModel.onAction(ForgotPasswordAction.Submit)
            assertEquals(ForgotPasswordEvent.NetworkError, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isEmailSent)
    }
}
