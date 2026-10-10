package com.example.huellitasya.feature.auth.register

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.RegisterUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.MissingFieldsFeedback
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    private fun viewModel(appScope: CoroutineScope) = RegisterViewModel(
        register = RegisterUseCase(authRepository, userRepository, appScope),
        validateName = ValidateNameUseCase(),
        validateEmail = ValidateEmailUseCase(),
        validatePassword = ValidatePasswordUseCase(),
    )

    private fun RegisterViewModel.fillValidForm(acceptTerms: Boolean = true) {
        onAction(RegisterAction.NameChanged("Santiago Cruz"))
        onAction(RegisterAction.EmailChanged("sa.company@hotmail.com"))
        onAction(RegisterAction.PasswordChanged("Huellitas1"))
        onAction(RegisterAction.ConfirmPasswordChanged("Huellitas1"))
        onAction(RegisterAction.TermsChanged(acceptTerms))
    }

    @Test
    fun `formulario vacio pide rellenar todos los datos`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(RegisterAction.Submit)

        val state = viewModel.uiState.value
        assertEquals(MissingFieldsFeedback, state.formFeedback)
        assertEquals(FieldStatus.Warning, state.nameFeedback?.status)
        assertEquals(FieldStatus.Warning, state.confirmPasswordFeedback?.status)
        assertFalse(state.isComplete)
        assertTrue(authRepository.registerCalls.isEmpty())
    }

    @Test
    fun `contrasenas distintas se marcan como error`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.fillValidForm()
        viewModel.onAction(RegisterAction.ConfirmPasswordChanged("Huellitas2"))

        viewModel.onAction(RegisterAction.Submit)

        val feedback = viewModel.uiState.value.confirmPasswordFeedback
        assertEquals(FieldStatus.Error, feedback?.status)
        assertEquals(ValidationError.PasswordsDoNotMatch.toUiText(), feedback?.message)
        assertNull(viewModel.uiState.value.formFeedback)
    }

    @Test
    fun `sin aceptar terminos no se registra`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.fillValidForm(acceptTerms = false)

        viewModel.onAction(RegisterAction.Submit)

        assertEquals(ValidationError.TermsNotAccepted.toUiText(), viewModel.uiState.value.termsFeedback?.message)
        assertTrue(authRepository.registerCalls.isEmpty())
    }

    @Test
    fun `correo existente se muestra bajo el correo`() = runTest {
        authRepository.registerResult = AppResult.Failure(AuthError.EmailAlreadyInUse)
        val viewModel = viewModel(appScope = this)
        viewModel.fillValidForm()

        viewModel.onAction(RegisterAction.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FieldStatus.Error, state.emailFeedback?.status)
        assertEquals(AuthError.EmailAlreadyInUse.toUiText(), state.emailFeedback?.message)
        assertFalse(state.isLoading)
    }

    @Test
    fun `registro valido crea la cuenta y el perfil`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.fillValidForm()
        assertTrue(viewModel.uiState.value.isComplete)

        viewModel.onAction(RegisterAction.Submit)
        advanceUntilIdle()

        assertEquals(1, authRepository.registerCalls.size)
        assertEquals(1, userRepository.createdUsers.size)
        assertTrue(viewModel.uiState.value.isLoading)
    }
}
