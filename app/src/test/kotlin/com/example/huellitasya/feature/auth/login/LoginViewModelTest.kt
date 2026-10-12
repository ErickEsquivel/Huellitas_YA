package com.example.huellitasya.feature.auth.login

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.usecase.SignInWithEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.FieldFeedback
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()

    private fun viewModel(appScope: CoroutineScope) = LoginViewModel(
        signInWithEmail = SignInWithEmailUseCase(authRepository, FakeUserRepository(), appScope),
        validateEmail = ValidateEmailUseCase(),
        validatePassword = ValidatePasswordUseCase(),
    )

    @Test
    fun `campos vacios marcan advertencias sin llamar a Firebase`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(LoginAction.Submit)

        val state = viewModel.uiState.value
        assertEquals(FieldFeedback(FieldStatus.Warning), state.emailFeedback)
        assertEquals(FieldFeedback(FieldStatus.Warning), state.passwordFeedback)
        assertEquals(MissingFieldsFeedback, state.formFeedback)
        assertTrue(authRepository.signInCalls.isEmpty())
    }

    @Test
    fun `editar un campo limpia su aviso`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(LoginAction.Submit)

        viewModel.onAction(LoginAction.EmailChanged("sa"))

        assertNull(viewModel.uiState.value.emailFeedback)
        assertNull(viewModel.uiState.value.formFeedback)
    }

    @Test
    fun `credenciales incorrectas se muestran bajo la contrasena`() = runTest {
        authRepository.signInResult = AppResult.Failure(AuthError.InvalidCredentials)
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(LoginAction.EmailChanged("sa.company@hotmail.com"))
        viewModel.onAction(LoginAction.PasswordChanged("mala"))

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FieldStatus.Error, state.passwordFeedback?.status)
        assertEquals(AuthError.InvalidCredentials.toUiText(), state.passwordFeedback?.message)
        assertFalse(state.isLoading)
    }

    @Test
    fun `error de red pide mostrar el overlay sin conexion`() = runTest {
        authRepository.signInResult = AppResult.Failure(AuthError.Network)
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(LoginAction.EmailChanged("sa.company@hotmail.com"))
        viewModel.onAction(LoginAction.PasswordChanged("secreto"))

        viewModel.events.test {
            viewModel.onAction(LoginAction.Submit)
            assertEquals(LoginEvent.NetworkError, awaitItem())
        }
    }

    @Test
    fun `login exitoso deja la carga activa hasta que la app cambie de grafo`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(LoginAction.EmailChanged("sa.company@hotmail.com"))
        viewModel.onAction(LoginAction.PasswordChanged("secreto"))

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, authRepository.signInCalls.size)
    }
}
