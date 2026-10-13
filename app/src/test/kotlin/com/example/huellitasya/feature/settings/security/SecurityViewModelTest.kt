package com.example.huellitasya.feature.settings.security

import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.usecase.ChangePasswordUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.testing.FakeAccountRepository
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.MissingFieldsFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SecurityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val accountRepository = FakeAccountRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel() = SecurityViewModel(
        changePassword = ChangePasswordUseCase(authRepository, accountRepository),
        validatePassword = ValidatePasswordUseCase(),
        networkMonitor = networkMonitor,
    )

    private fun SecurityViewModel.fill(current: String, new: String, confirmation: String = new) {
        onAction(SecurityAction.CurrentPasswordChanged(current))
        onAction(SecurityAction.NewPasswordChanged(new))
        onAction(SecurityAction.ConfirmationChanged(confirmation))
    }

    @Test
    fun `campos vacios marcan avisos sin llamar a Firebase`() {
        val viewModel = viewModel()

        viewModel.onAction(SecurityAction.Submit)

        val state = viewModel.uiState.value
        assertEquals(FieldStatus.Warning, state.currentFeedback?.status)
        assertEquals(MissingFieldsFeedback, state.formFeedback)
        assertTrue(authRepository.reauthPasswords.isEmpty())
    }

    @Test
    fun `la checklist se actualiza en vivo y el boton se pone verde`() {
        val viewModel = viewModel()

        viewModel.fill(current = "Actual1", new = "Nueva2025")

        val state = viewModel.uiState.value
        assertTrue(state.requirements.allMet)
        assertTrue(state.isComplete)
    }

    @Test
    fun `contrasenas distintas no se envian`() {
        val viewModel = viewModel()
        viewModel.fill(current = "Actual1", new = "Nueva2025", confirmation = "Otra2025")

        viewModel.onAction(SecurityAction.Submit)

        assertEquals(FieldStatus.Error, viewModel.uiState.value.confirmationFeedback?.status)
        assertTrue(authRepository.updatedPasswords.isEmpty())
    }

    @Test
    fun `la nueva debe ser distinta a la actual`() {
        val viewModel = viewModel()
        viewModel.fill(current = "Nueva2025", new = "Nueva2025")

        viewModel.onAction(SecurityAction.Submit)

        assertEquals(
            UiText.Resource(R.string.error_password_same),
            viewModel.uiState.value.newFeedback?.message,
        )
        assertTrue(authRepository.reauthPasswords.isEmpty())
    }

    @Test
    fun `cambio valido limpia el formulario y avisa`() = runTest {
        val viewModel = viewModel()
        viewModel.fill(current = "Actual1", new = "Nueva2025")

        viewModel.events.test {
            viewModel.onAction(SecurityAction.Submit)
            assertEquals(SecurityEvent.ShowMessage(UiText.Resource(R.string.security_success)), awaitItem())
        }

        assertEquals(listOf("Nueva2025"), authRepository.updatedPasswords)
        assertEquals(1, accountRepository.passwordNoticeCount)
        assertEquals("", viewModel.uiState.value.currentPassword)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `contrasena actual incorrecta se marca bajo el campo como en Figma`() {
        authRepository.reauthResult = AppResult.Failure(AuthError.InvalidCredentials)
        val viewModel = viewModel()
        viewModel.fill(current = "Mala1234", new = "Nueva2025")

        viewModel.onAction(SecurityAction.Submit)

        val feedback = viewModel.uiState.value.currentFeedback
        assertEquals(FieldStatus.Error, feedback?.status)
        assertEquals(UiText.Resource(R.string.error_current_password_incorrect), feedback?.message)
        assertTrue(authRepository.updatedPasswords.isEmpty())
    }

    @Test
    fun `sin conexion avisa y no intenta cambiarla`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel()
        viewModel.fill(current = "Actual1", new = "Nueva2025")

        viewModel.events.test {
            viewModel.onAction(SecurityAction.Submit)
            assertEquals(SecurityEvent.ShowMessage(AuthError.Network.toUiText()!!), awaitItem())
        }
        assertTrue(authRepository.reauthPasswords.isEmpty())
    }
}
