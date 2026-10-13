package com.example.huellitasya.feature.auth.newpassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.usecase.ResetPasswordUseCase
import com.example.huellitasya.core.domain.validation.PasswordRequirements
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.testing.FakePasswordResetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.MissingFieldsFeedback
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NewPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePasswordResetRepository()
    private val viewModel = NewPasswordViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                NewPasswordViewModel.EMAIL_ARG to "sa.company@hotmail.com",
                NewPasswordViewModel.RESET_TOKEN_ARG to "token-123",
            )
        ),
        resetPassword = ResetPasswordUseCase(repository),
        validatePassword = ValidatePasswordUseCase(),
    )

    @Test
    fun `la checklist se actualiza mientras se escribe`() {
        viewModel.onAction(NewPasswordAction.PasswordChanged("huellitas"))
        assertEquals(PasswordRequirements(true, false, false, false), viewModel.uiState.value.requirements)

        viewModel.onAction(NewPasswordAction.PasswordChanged("Huellitas1"))
        viewModel.onAction(NewPasswordAction.ConfirmationChanged("Huellitas1"))

        assertEquals(PasswordRequirements(true, true, true, true), viewModel.uiState.value.requirements)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `campos vacios piden rellenar los datos`() {
        viewModel.onAction(NewPasswordAction.Submit)

        assertEquals(MissingFieldsFeedback, viewModel.uiState.value.formFeedback)
        assertEquals(FieldStatus.Warning, viewModel.uiState.value.passwordFeedback?.status)
        assertTrue(repository.resetCalls.isEmpty())
    }

    @Test
    fun `contrasenas distintas marcan la confirmacion sin llamar al servidor`() {
        viewModel.onAction(NewPasswordAction.PasswordChanged("Huellitas1"))
        viewModel.onAction(NewPasswordAction.ConfirmationChanged("Huellitas2"))

        viewModel.onAction(NewPasswordAction.Submit)

        assertEquals(FieldStatus.Error, viewModel.uiState.value.confirmationFeedback?.status)
        assertEquals(null, viewModel.uiState.value.passwordFeedback)
        assertTrue(repository.resetCalls.isEmpty())
    }

    @Test
    fun `contrasena valida se cambia con el token y vuelve a login`() = runTest {
        viewModel.onAction(NewPasswordAction.PasswordChanged("Huellitas1"))
        viewModel.onAction(NewPasswordAction.ConfirmationChanged("Huellitas1"))

        viewModel.events.test {
            viewModel.onAction(NewPasswordAction.Submit)
            assertEquals(NewPasswordEvent.PasswordReset, awaitItem())
        }
        assertEquals(listOf(Triple("sa.company@hotmail.com", "token-123", "Huellitas1")), repository.resetCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `si la verificacion vencio se informa`() = runTest {
        repository.resetResult = AppResult.Failure(PasswordResetError.ExpiredCode)
        viewModel.onAction(NewPasswordAction.PasswordChanged("Huellitas1"))
        viewModel.onAction(NewPasswordAction.ConfirmationChanged("Huellitas1"))

        viewModel.events.test {
            viewModel.onAction(NewPasswordAction.Submit)
            assertEquals(NewPasswordEvent.ShowMessage(PasswordResetError.ExpiredCode.toUiText()!!), awaitItem())
        }
    }
}
